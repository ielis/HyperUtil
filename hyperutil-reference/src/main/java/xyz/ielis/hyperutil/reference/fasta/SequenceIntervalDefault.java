package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.util.Objects;
import java.util.Optional;

/**
 * This class puts together a nucleotide sequence and a particular {@link GenomeInterval}. Each instance is built
 * using the builder while checks are performed at the end of the building process.
 */
class SequenceIntervalDefault implements SequenceInterval {

    private final GenomeInterval interval;
    private final String sequence;

    protected SequenceIntervalDefault(GenomeInterval interval, String sequence) {
        this.interval = Objects.requireNonNull(interval, "Interval cannot be null");
        this.sequence = Objects.requireNonNull(sequence, "Sequence cannot be null");
        if (interval.length() != sequence.length()) {
            throw new IllegalArgumentException(String.format("Lengths do not match: interval %s != sequence %s",
                    interval.length(), sequence.length()));
        }
    }

    static SequenceIntervalDefault of(GenomeInterval interval, String sequence) {
        return new SequenceIntervalDefault(interval, sequence);
    }

    /**
     * Convert nucleotide sequence to reverse complement. If <code>U</code> is present, it is complemented to
     * <code>A</code> as expected. However, <code>A</code> is always complemented to <code>T</code>.
     *
     * @param sequence of nucleotides in IUPAC notation
     * @return reverse complement of given <code>sequence</code>
     */
    static String reverseComplement(String sequence) {
        return ReverseComplement.reverseComplement(sequence);
    }

    @Override
    public GenomeInterval getInterval() {
        return interval;
    }

    @Override
    public String getSequence() {
        return sequence;
    }

    /**
     * Get sequence present within given {@code interval}.
     *
     * @param interval to get subsequence for
     * @return Optional with subsequence String, empty if {@code interval} is not contained within this sequence
     */
    @Override
    public Optional<String> getSubsequence(GenomeInterval interval) {
        if (this.interval.contains(interval)) {
            GenomeInterval onStrand = interval.withStrand(this.interval.getStrand());
            String seq = sequence.substring(onStrand.getBeginPos() - this.interval.getBeginPos(),
                    onStrand.getEndPos() - this.interval.getBeginPos());
            return interval.getStrand().equals(this.interval.getStrand())
                    ? Optional.of(seq)
                    : Optional.of(reverseComplement(seq));
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceIntervalDefault that = (SequenceIntervalDefault) o;
        return Objects.equals(interval, that.interval) &&
                Objects.equals(sequence, that.sequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, sequence);
    }


    @Override
    public String toString() {
        return "SEQ{ " + interval +
                ", '" + sequence + "'}";
    }
}
