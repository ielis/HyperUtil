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
    private final SAMSequenceDictionary sequenceDictionary;

    private final ReferenceDictionary referenceDictionary;

    SingleFastaGenomeSequenceAccessor(Path fastaPath) {
        this(fastaPath,
                fastaPath.resolveSibling(fastaPath.toFile().getName() + ".fai"),
                fastaPath.resolveSibling(fastaPath.toFile().getName() + ".dict"));
    }

    SingleFastaGenomeSequenceAccessor(Path fastaPath, Path fastaFai, Path fastaDict) {
        this.fasta = new IndexedFastaSequenceFile(fastaPath, new FastaSequenceIndex(fastaFai));
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

    /**
     * Convert nucleotide sequence to reverse complement.
     *
     * @param sequence of nucleotides, only {a,c,g,t,n,A,C,G,T,N} permitted.
     * @return reverse complement of given sequence
     */
    private static String reverseComplement(String sequence) {
        char[] oldSeq = sequence.toCharArray();
        char[] newSeq = new char[oldSeq.length];
        int idx = oldSeq.length - 1;
        for (int i = 0; i < oldSeq.length; i++) {
            if (oldSeq[i] == 'A') {
                newSeq[idx - i] = 'T';
            } else if (oldSeq[i] == 'a') {
                newSeq[idx - i] = 't';
            } else if (oldSeq[i] == 'T') {
                newSeq[idx - i] = 'A';
            } else if (oldSeq[i] == 't') {
                newSeq[idx - i] = 'a';
            } else if (oldSeq[i] == 'C') {
                newSeq[idx - i] = 'G';
            } else if (oldSeq[i] == 'c') {
                newSeq[idx - i] = 'g';
            } else if (oldSeq[i] == 'G') {
                newSeq[idx - i] = 'C';
            } else if (oldSeq[i] == 'g') {
                newSeq[idx - i] = 'c';
            } else if (oldSeq[i] == 'N') {
                newSeq[idx - i] = 'N';
            } else if (oldSeq[i] == 'n') {
                newSeq[idx - i] = 'n';
            } else throw new IllegalArgumentException(String.format("Illegal nucleotide %s in sequence %s",
                    oldSeq[i], sequence));
        }
        return new String(newSeq);
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
            throw new InvalidFastaFileException("Missing mitochondrial contig among contigs "
                    + sequenceDictionary.getSequences().stream()
                    .map(SAMSequenceRecord::getSequenceName)
                    .collect(Collectors.joining(",", "{", "}")));
        }

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

        return rdb.build();
    }

    /**
     * Extract nucleotide sequence from reference genome fasta file that is lying inside given {@link GenomeInterval}.
     *
     * @param interval where the nucleotide sequence will be extracted from.
     * @return nucleotide sequence
     */
    @Override
    public String fetchSequence(GenomeInterval interval) {
        String chrom = interval.getRefDict().getContigIDToName().get(interval.getChr());
        int begin, end;
        switch (interval.getStrand()) {
            case FWD:
                begin = interval.getBeginPos() + 1; // convert to 1-based pos
                end = interval.getEndPos();
                return fetchSequence(chrom, begin, end);
            case REV:
                GenomeInterval fwd = interval.withStrand(Strand.FWD);
                begin = fwd.getBeginPos() + 1;
                end = fwd.getEndPos();
                return reverseComplement(fetchSequence(chrom, begin, end));
            default:
                throw new IllegalArgumentException(String.format("Unknown strand %s", interval.getStrand()));
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
