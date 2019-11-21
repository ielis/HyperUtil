package xyz.ielis.hyperutil.reference.fasta;

import java.nio.file.Path;

public class GenomeSequenceAccessors {

    private GenomeSequenceAccessors() {
        // private no-op
    }

    public static GenomeSequenceAccessor getGenomeSequenceAccessor(Path fastaPath) {
        return getGenomeSequenceAccessor(fastaPath, Type.SINGLE_FASTA);
    }

    public static GenomeSequenceAccessor getGenomeSequenceAccessor(Path fastaPath, Type type) {
        switch (type) {
            case SINGLE_FASTA:
                return new SingleFastaGenomeSequenceAccessor(fastaPath);
            case SINGLE_CHROMOSOME:
                return new SingleChromosomeGenomeSequenceAccessor(fastaPath);
            default:
                throw new IllegalArgumentException(String.format("Unknown type `%s`", type));
        }
    }

    public enum Type {
        SINGLE_FASTA,
        SINGLE_CHROMOSOME
    }
}
