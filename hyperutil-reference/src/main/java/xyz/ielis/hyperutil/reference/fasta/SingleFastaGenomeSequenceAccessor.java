package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.data.ReferenceDictionaryBuilder;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.variant.utils.SAMSequenceDictionaryExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class allows to extract arbitrary sequence from reference genome. To do so it requires single fasta file that
 * contains all contigs.
 *
 * <p>
 * Fasta index and dictionary are required to be present in directory. The index is created by
 * <code>samtools faidx file.fa</code>. The dictionary is created by <code>samtools dict file.fa &gt; file.fa.dict</code>
 * </p>
 * <p>
 * Created by Daniel Danis on 11/18/19.
 * </p>
 */
public class SingleFastaGenomeSequenceAccessor implements GenomeSequenceAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleFastaGenomeSequenceAccessor.class);


    protected final IndexedFastaSequenceFile fasta;
    /**
     * True if all chromosomes in FASTA are prefixed with `chr` and false if all chromosomes are not prefixed.
     */
    private final boolean usesPrefix;

    /**
     * True if `chrM`, `chrMT`, `M`, or `MT` must be present. Otherwise an exception is thrown
     */
    private final boolean requireMt;

    private final SAMSequenceDictionary sequenceDictionary;

    private final ReferenceDictionary referenceDictionary;

    SingleFastaGenomeSequenceAccessor(Path fastaPath, Path fastaFai, Path fastaDict) {
        this(fastaPath, fastaFai, fastaDict, true);
    }

    SingleFastaGenomeSequenceAccessor(Path fastaPath, Path fastaFai, Path fastaDict, boolean requireMt) {
        this.fasta = new IndexedFastaSequenceFile(fastaPath, new FastaSequenceIndex(fastaFai));
        this.requireMt = requireMt;
        this.sequenceDictionary = buildSequenceDictionary(fastaDict);
        this.usesPrefix = figureOutPrefix(sequenceDictionary);
        this.referenceDictionary = buildReferenceDictionary(sequenceDictionary);
    }

    private static boolean figureOutPrefix(SAMSequenceDictionary sequenceDictionary) {
        Predicate<SAMSequenceRecord> prefixed = e -> e.getSequenceName().startsWith("chr");
        boolean allPrefixed = sequenceDictionary.getSequences().stream().allMatch(prefixed);
        boolean nonePrefixed = sequenceDictionary.getSequences().stream().noneMatch(prefixed);

        if (allPrefixed) {
            return true;
        } else if (nonePrefixed) {
            return false;
        } else {
            String msg = String.format("Found prefixed and unprefixed contigs among fasta dictionary entries - %s",
                    sequenceDictionary.getSequences().stream()
                            .map(SAMSequenceRecord::getSequenceName).collect(Collectors.joining(",", "{", "}")));
            LOGGER.error(msg);
            throw new InvalidFastaFileException(msg);
        }
    }

    private static SAMSequenceDictionary buildSequenceDictionary(Path dictPath) {
        return SAMSequenceDictionaryExtractor.extractDictionary(dictPath);
    }

    private ReferenceDictionary buildReferenceDictionary(SAMSequenceDictionary sequenceDictionary) {
        ReferenceDictionaryBuilder rdb = new ReferenceDictionaryBuilder();
        for (int i = 0; i < sequenceDictionary.getSequences().size(); i++) {
            SAMSequenceRecord seq = sequenceDictionary.getSequences().get(i);
            final String sequenceName = seq.getSequenceName();

            final String noChr, withChr;
            // make sure there are both version `chrX` and `X` present
            if (sequenceName.startsWith("chr")) {
                withChr = sequenceName;
                noChr = sequenceName.substring(3);
            } else {
                withChr = "chr" + sequenceName;
                noChr = sequenceName;
            }
            rdb.putContigID(withChr, i);
            rdb.putContigID(noChr, i);

            rdb.putContigName(i, usesPrefix ? withChr : noChr);

            rdb.putContigLength(i, seq.getSequenceLength());
        }

        // if chrMT is being used, then add chrM, M, and vice versa
        final String mt = usesPrefix ? "chrMT" : "MT";
        final String m = usesPrefix ? "chrM" : "M";

        final Integer mtId = rdb.getContigID(mt) != null ? rdb.getContigID(mt) : rdb.getContigID(m);
        if (mtId == null) {
            if (requireMt) {
                throw new InvalidFastaFileException("Missing mitochondrial contig among contigs "
                        + sequenceDictionary.getSequences().stream()
                        .map(SAMSequenceRecord::getSequenceName)
                        .collect(Collectors.joining(",", "{", "}")));
            }
            // do not process mitochondrial chromosome
        } else {
            final String mtName = rdb.getContigName(mtId);
            if (mtName.contains("MT")) {
                // builder already contains `MT` version, we need to add `M`
                rdb.putContigID("chrM", mtId);
                rdb.putContigID("M", mtId);
                // and length should already present in the rd builder
            } else if (mtName.contains("M")) {
                // builder already contains `M` version, we need to add `MT`
                rdb.putContigID("chrMT", mtId);
                rdb.putContigID("MT", mtId);
                // again, length should already present in the rd builder
            } else {
                throw new InvalidFastaFileException("Unexpected name of mitochondrial contig " + mtName);
            }
        }

        return rdb.build();
    }

    /**
     * Extract nucleotide sequence from reference genome fasta file that is lying inside given {@link GenomeInterval}.
     *
     * @param query where the nucleotide sequence will be extracted from.
     * @return nucleotide sequence
     */
    @Override
    public Optional<SequenceInterval> fetchSequence(GenomeInterval query) {
        String queryContigName = query.getRefDict().getContigIDToName().get(query.getChr());
        if (!referenceDictionary.getContigNameToID().containsKey(queryContigName)) {
            LOGGER.warn("Unknown chromosome `{}`", queryContigName);
            return Optional.empty();
        }

        // the name we use for contig in FASTA file
        int primaryContigId = referenceDictionary.getContigNameToID().get(queryContigName);
        String primaryContigName = referenceDictionary.getContigIDToName().get(primaryContigId);
        GenomeInterval onStrand = query.withStrand(Strand.FWD);
        String seq = fetchSequence(primaryContigName, onStrand.getBeginPos() + 1, onStrand.getEndPos());
        switch (query.getStrand()) {
            case FWD:
                return Optional.of(SequenceInterval.builder()
                        .interval(query)
                        .sequence(seq)
                        .build());
            case REV:
                return Optional.of(SequenceInterval.builder()
                        .interval(query)
                        .sequence(SequenceInterval.reverseComplement(seq))
                        .build());
            default:
                throw new IllegalArgumentException(String.format("Unknown strand `%s`", query.getStrand()));
        }
    }

    @Override
    public ReferenceDictionary getReferenceDictionary() {
        return referenceDictionary;
    }

    /**
     * Get sequence of nucleotides from given position specified by chromosome/contig name, starting position and ending
     * position. Case of nucleotides is not changed.
     *
     * @param chr   chromosome
     * @param start start position in 1-based numbering
     * @param end   end chromosomal position in 1-based numbering
     * @return nucleotide sequence
     */
    @Override
    public String fetchSequence(String chr, int start, int end) throws SAMException {
        // Fasta from UCSC has prefix chr, hence we need to have it there from now on
        String chrom = (chr.startsWith("chr")) ? chr : "chr" + chr;
        ReferenceSequence referenceSequence = fasta.getSubsequenceAt(chrom, start, end);
        return new String(referenceSequence.getBases());
    }

    @Override
    public void close() throws IOException {
        this.fasta.close();
    }
}
