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

import java.io.IOException;
import java.nio.file.Path;

/**
 * This class allows to extract arbitrary sequence from reference genome. To do so it requires single fasta file that
 * contains all contigs. Fasta index is required to be present in directory, create it using <code>samtools faidx
 * file.fa</code>.
 * Created by Daniel Danis on 11/18/19.
 */
public class SingleFastaGenomeSequenceAccessor implements GenomeSequenceAccessor {

    private final IndexedFastaSequenceFile fasta;

    private final SAMSequenceDictionary sequenceDictionary;

    private final ReferenceDictionary referenceDictionary;

    public SingleFastaGenomeSequenceAccessor(Path fastaPath) {
        this(fastaPath,
                fastaPath.resolveSibling(fastaPath.toFile().getName() + ".fai"),
                fastaPath.resolveSibling(fastaPath.toFile().getName() + ".dict"));
    }

    public SingleFastaGenomeSequenceAccessor(Path fastaPath, Path fastaFai, Path fastaDict) {
        this.fasta = new IndexedFastaSequenceFile(fastaPath, new FastaSequenceIndex(fastaFai));
        this.sequenceDictionary = buildSequenceDictionary(fastaDict);
        this.referenceDictionary = buildReferenceDictionary(sequenceDictionary);
    }

    private static ReferenceDictionary buildReferenceDictionary(SAMSequenceDictionary sequenceDictionary) {
        ReferenceDictionaryBuilder rdb = new ReferenceDictionaryBuilder();
        for (int i = 0; i < sequenceDictionary.getSequences().size(); i++) {
            SAMSequenceRecord seq = sequenceDictionary.getSequences().get(i);
            rdb.putContigID(seq.getSequenceName(), i);
            rdb.putContigName(i, seq.getSequenceName());
            rdb.putContigLength(i, seq.getSequenceLength());
        }

        return rdb.build();
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
