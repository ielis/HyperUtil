package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.util.Optional;

/**
 * Container for a reference sequence stored as a String and a {@link GenomeInterval}.
 */
public interface SequenceInterval {

    static SequenceInterval empty() {
        return SequenceIntervalEmpty.instance();
    }

    GenomeInterval getInterval();

    String getSequence();

    Optional<String> getSubsequence(GenomeInterval interval);

    default boolean isEmpty() {
        return this.equals(empty());
    }
}
