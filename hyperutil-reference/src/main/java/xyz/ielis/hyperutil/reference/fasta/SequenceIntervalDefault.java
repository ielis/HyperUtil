package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This class puts together a nucleotide sequence and a particular {@link GenomeInterval}. Each instance is built
 * using the builder while checks are performed at the end of the building process.
 */
public class SequenceIntervalDefault implements SequenceInterval {

    /**
     * Map for translation of a single nucleotide/base symbol into its reverse complement base
     */
    private static final Map<Character, Character> IUPAC_COMPLEMENT_MAP;

    static {
        Map<Character, Character> TEMPORARY = new HashMap<>();
        TEMPORARY.putAll(
                Map.of(
                        // STANDARD
                        'A', 'T',
                        'C', 'G',
                        'G', 'C',
                        'T', 'A',
                        'U', 'A'));
        TEMPORARY.putAll(
                Map.of(
                        // AMBIGUITY BASES - first part
                        'W', 'W', // weak - A,T
                        'S', 'S', // strong - C,G
                        'M', 'K', // amino - A,C
                        'K', 'M', // keto - G,T
                        'R', 'Y', // purine - A,G
                        'Y', 'R')); // pyrimidine - C,T

        TEMPORARY.putAll(
                Map.of(
                        // AMBIGUITY BASES - second part
                        'B', 'V', // not A
                        'D', 'H', // not C
                        'H', 'D', // not G
                        'V', 'B', // not T
                        'N', 'N' // any one base
                )
        );

        IUPAC_COMPLEMENT_MAP = Map.copyOf(TEMPORARY);
    }


    private final GenomeInterval interval;
    private final String sequence;

    private SequenceIntervalDefault(Builder builder) {
        interval = Objects.requireNonNull(builder.interval, "Interval cannot be null");
        sequence = Objects.requireNonNull(builder.sequence, "Sequence cannot be null");
        if (interval.length() != sequence.length()) {
            throw new IllegalArgumentException(String.format("Lengths do not match: interval %s != sequence %s",
                    interval.length(), sequence.length()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convert nucleotide sequence to reverse complement. If <code>U</code> is present, it is complemented to
     * <code>A</code> as expected. However, <code>A</code> is always complemented to <code>T</code>.
     *
     * @param sequence of nucleotides in IUPAC notation
     * @return reverse complement of given <code>sequence</code>
     */
    static String reverseComplement(String sequence) {
        char[] oldSeq = sequence.toCharArray();
        char[] newSeq = new char[oldSeq.length];
        int idx = oldSeq.length - 1;
        for (int i = 0; i < oldSeq.length; i++) {
            char template = oldSeq[i];
            boolean isUpperCase = Character.isUpperCase(template);

            char toLookUp = isUpperCase
                    ? template // no-op, the IUPAC map contains upper-case characters
                    : Character.toUpperCase(template);

            char complement = IUPAC_COMPLEMENT_MAP.getOrDefault(toLookUp, template);
            char complementWithCase = isUpperCase
                    ? complement // no-op, the IUPAC map contains upper-case characters
                    : Character.toLowerCase(complement);

            newSeq[idx - i] = complementWithCase;
        }
        return new String(newSeq);
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
        return "SequenceInterval{" +
                "interval=" + interval +
                ", sequence='" + sequence + '\'' +
                '}';
    }

    public static final class Builder {
        private GenomeInterval interval;
        private String sequence;

        private Builder() {
        }

        public Builder interval(GenomeInterval interval) {
            this.interval = interval;
            return this;
        }

        public Builder sequence(String sequence) {
            this.sequence = sequence;
            return this;
        }

        /**
         * @return built {@link SequenceIntervalDefault}
         * @throws IllegalArgumentException if the {@link #sequence} length does not match length of the provided
         *                                  {@link #interval}
         */
        public SequenceIntervalDefault build() {
            return new SequenceIntervalDefault(this);
        }
    }
}
