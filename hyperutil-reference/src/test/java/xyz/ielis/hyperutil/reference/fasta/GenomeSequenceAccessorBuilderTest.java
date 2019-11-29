package xyz.ielis.hyperutil.reference.fasta;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenomeSequenceAccessorBuilderTest {

    private static final Path FASTA = Paths.get(GenomeSequenceAccessorBuilderTest.class.getResource("small_hg19.fa").getPath());
    private static final Path FASTA_FAI = Paths.get(GenomeSequenceAccessorBuilderTest.class.getResource("small_hg19.fa.fai").getPath());
    private static final Path FASTA_DICT = Paths.get(GenomeSequenceAccessorBuilderTest.class.getResource("small_hg19.fa.dict").getPath());

    @Test
    void buildWhenAllArgumentsArePresent() {
        GenomeSequenceAccessor accessor = GenomeSequenceAccessorBuilder.builder()
                .setFastaPath(FASTA)
                .setFastaFaiPath(FASTA_FAI)
                .setFastaDictPath(FASTA_DICT)
                .setType(GenomeSequenceAccessor.Type.SINGLE_FASTA)
                .setRequireMt(true)
                .build();

        assertThat(accessor, is(instanceOf(SingleFastaGenomeSequenceAccessor.class)));
        assertThat(accessor.getReferenceDictionary().getContigNameToID().keySet(), hasItems("chr1", "1", "chr2", "2", "M", "chrM", "chrMT", "MT"));
    }

    @Test
    void buildSingleChromosomeAccessor() {
        GenomeSequenceAccessor accessor = GenomeSequenceAccessorBuilder.builder()
                .setFastaPath(FASTA)
                .setFastaFaiPath(FASTA_FAI)
                .setFastaDictPath(FASTA_DICT)
                .setType(GenomeSequenceAccessor.Type.SINGLE_CHROMOSOME)
                .setRequireMt(true).build();

        assertThat(accessor, is(instanceOf(SingleChromosomeGenomeSequenceAccessor.class)));
    }

    @Test
    void buildWithDefaultArguments() {
        final GenomeSequenceAccessor accessor = GenomeSequenceAccessorBuilder.builder()
                .setFastaPath(FASTA)
                .build();

        assertThat(accessor, is(instanceOf(SingleFastaGenomeSequenceAccessor.class)));
    }

    @Test
    void failsWhenNonExistingFileIsUsed() {
        assertThrows(IllegalArgumentException.class,
                () -> GenomeSequenceAccessorBuilder.builder()
                        .setFastaPath(FASTA.getParent())
                        .build());
    }

    @Test
    void failsWhenFaiIsNotPresent() {
        assertThrows(IllegalArgumentException.class,
                () -> GenomeSequenceAccessorBuilder.builder()
                        .setFastaPath(Paths.get(GenomeSequenceAccessorBuilderTest.class.getResource("small_hg19_1.fa").getPath()))
                        .setFastaDictPath(FASTA_DICT)
                        .build());
    }

    @Test
    void failsWhenDictIsNotPresent() {
        assertThrows(IllegalArgumentException.class,
                () -> GenomeSequenceAccessorBuilder.builder()
                        .setFastaPath(Paths.get(GenomeSequenceAccessorBuilderTest.class.getResource("small_hg19_1.fa").getPath()))
                        .setFastaFaiPath(FASTA_FAI)
                        .build());
    }
}