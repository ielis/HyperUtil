package xyz.ielis.hyperutil.reference.fasta;

import java.nio.file.Path;

@Deprecated(forRemoval = true)
public class GenomeSequenceAccessors {

    private GenomeSequenceAccessors() {
        // private no-op
    }

    public static GenomeSequenceAccessor getGenomeSequenceAccessor(Path fastaPath) {
        return getGenomeSequenceAccessor(fastaPath, GenomeSequenceAccessor.Type.SINGLE_FASTA);
    }

    public static GenomeSequenceAccessor getGenomeSequenceAccessor(Path fastaPath, GenomeSequenceAccessor.Type type) {
        final Path expectedFai = fastaPath.resolveSibling(fastaPath.toFile().getName() + ".fai");
        final Path expectedDict = fastaPath.resolveSibling(fastaPath.toFile().getName() + ".dict");
        switch (type) {
            case SINGLE_FASTA:
                return new SingleFastaGenomeSequenceAccessor(fastaPath, expectedFai, expectedDict);
            case SINGLE_CHROMOSOME:
                return new SingleChromosomeGenomeSequenceAccessor(fastaPath, expectedFai, expectedDict);
            default:
                throw new IllegalArgumentException(String.format("Unknown type `%s`", type));
        }
    }
}
