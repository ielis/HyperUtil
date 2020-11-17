package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.util.Optional;

class SequenceIntervalEmpty implements SequenceInterval {

    private static final SequenceIntervalEmpty INSTANCE = new SequenceIntervalEmpty();

    private SequenceIntervalEmpty() {
        // private no-op
    }

    static SequenceIntervalEmpty instance() {
        return INSTANCE;
    }

    @Override

    public GenomeInterval getInterval() {
        return null;
    }

    @Override
    public String getSequence() {
        return "";
    }

    @Override
    public Optional<String> getSubsequence(GenomeInterval interval) {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "EMPTY_SEQ";
    }
}
