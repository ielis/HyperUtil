package xyz.ielis.hyperutil.reference.fasta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class GenomeSequenceAccessorBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenomeSequenceAccessorBuilder.class);

    private Path fastaPath;

    private Path fastaFaiPath;

    private Path fastaDictPath;

    private GenomeSequenceAccessor.Type type = GenomeSequenceAccessor.Type.SINGLE_FASTA;

    private boolean requireMt = true;

    private GenomeSequenceAccessorBuilder() {
        // private no-op
    }

    public static GenomeSequenceAccessorBuilder builder() {
        return new GenomeSequenceAccessorBuilder();
    }

    /**
     * Set path to fasta file (REQUIRED).
     *
     * @param fastaPath path to fasta file
     * @return builder
     */
    public GenomeSequenceAccessorBuilder setFastaPath(Path fastaPath) {
        this.fastaPath = fastaPath;
        return this;
    }

    /**
     * Path to fasta index, `some.fa.fai` is tried for `some.fa` if explicit path is not provided.
     *
     * @param fastaFaiPath path to fasta index
     * @return builder
     */
    public GenomeSequenceAccessorBuilder setFastaFaiPath(Path fastaFaiPath) {
        this.fastaFaiPath = fastaFaiPath;
        return this;
    }

    /**
     * Path to sequence dictionary, `some.fa.dict` is tried for `some.fa` if explicit path is not provided.
     *
     * @param fastaDictPath path to fasta sequence dictionary
     * @return builder
     */
    public GenomeSequenceAccessorBuilder setFastaDictPath(Path fastaDictPath) {
        this.fastaDictPath = fastaDictPath;
        return this;
    }

    /**
     * @param type type of the requested accessor, {@link GenomeSequenceAccessor.Type#SINGLE_FASTA} by default
     * @return builder
     */
    public GenomeSequenceAccessorBuilder setType(GenomeSequenceAccessor.Type type) {
        this.type = type;
        return this;
    }

    /**
     * If set to true, then mitochondrial chromosome must be present in the fasta file.
     *
     * @param requireMt true if mitochondrial chromosome must be present in the fasta file
     * @return builder
     */
    public GenomeSequenceAccessorBuilder setRequireMt(boolean requireMt) {
        this.requireMt = requireMt;
        return this;
    }

    /**
     * Process arguments and return the accessor. Throws {@link IllegalArgumentException} if there are any problems with
     * provided arguments.
     *
     * @return {@link GenomeSequenceAccessor}
     */
    public GenomeSequenceAccessor build() {
        // we need fasta path
        if (!fastaPath.toFile().isFile()) {
            throw new IllegalArgumentException(String.format("%s does not exist", fastaPath));
        }

        // fasta index (FAI)
        if (fastaFaiPath == null) {
            final Path expectedFaiPath = fastaPath.resolveSibling(fastaPath.toFile().getName() + ".fai");
            if (expectedFaiPath.toFile().isFile()) {
                LOGGER.debug("Found fasta index at `{}`", expectedFaiPath);
                this.fastaFaiPath = expectedFaiPath;
            } else {
                throw new IllegalArgumentException(String.format("Path to fasta index unset and did not find the index at `%s`", expectedFaiPath));
            }
        }

        // fasta dictionary (DICT)
        if (fastaDictPath == null) {
            final Path expectedDictPath = fastaPath.resolveSibling(fastaPath.toFile().getName() + ".dict");
            if (expectedDictPath.toFile().isFile()) {
                LOGGER.debug("Found fasta dictionary at `{}`", expectedDictPath);
                this.fastaDictPath = expectedDictPath;
            } else {
                throw new IllegalArgumentException(String.format("Path to fasta dictionary unset and did not find the dict at `%s`", expectedDictPath));
            }
        }

        switch (type) {
            case SINGLE_CHROMOSOME:
                return new SingleChromosomeGenomeSequenceAccessor(fastaPath, fastaFaiPath, fastaDictPath, requireMt);
            case SINGLE_FASTA:
                return new SingleFastaGenomeSequenceAccessor(fastaPath, fastaFaiPath, fastaDictPath, requireMt);
            default:
                throw new IllegalArgumentException(String.format("Unknown type `%s`", type));
        }
    }
}
