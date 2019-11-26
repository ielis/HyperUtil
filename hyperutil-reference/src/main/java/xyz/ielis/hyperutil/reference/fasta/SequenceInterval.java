package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class SequenceInterval {

    private static final Pattern BASES = Pattern.compile("[ACGTNacgtn]+");

    private final GenomeInterval interval;
    private final String sequence;

    private SequenceInterval(Builder builder) {
        interval = Objects.requireNonNull(builder.interval, "Interval cannot be null");
        sequence = Objects.requireNonNull(builder.sequence, "Sequence cannot be null");
        if (interval.length() != sequence.length()) {
            throw new IllegalArgumentException(String.format("Lengths do not match: interval %s != sequence %s",
                    interval.length(), sequence.length()));
        }
        if (!BASES.matcher(sequence).matches()) {
            throw new IllegalArgumentException("Sequence does not match regexp `[ACGTNacgtn]+`");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convert nucleotide sequence to reverse complement.
     *
     * @param sequence of nucleotides, only {a,c,g,t,n,A,C,G,T,N} permitted
     * @return reverse complement of given <code>sequence</code>
     * @throws IllegalArgumentException if there is an unpermitted character present
     */
    static String reverseComplement(String sequence) {
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
            } else
                throw new IllegalArgumentException(String.format("Illegal nucleotide %s in sequence %s",
                        oldSeq[i], sequence));
        }
        return new String(newSeq);
    }

    public GenomeInterval getInterval() {
        return interval;
    }

    public String getSequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceInterval that = (SequenceInterval) o;
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

    /**
     * Get sequence present within given {@code interval}.
     *
     * @param interval to get subsequence for
     * @return Optional with subsequence String, empty if {@code interval} is not contained within this sequence
     */
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

        public SequenceInterval build() {
            return new SequenceInterval(this);
        }
    }
}
