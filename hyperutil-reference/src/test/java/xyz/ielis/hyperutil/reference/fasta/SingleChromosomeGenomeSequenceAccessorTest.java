package xyz.ielis.hyperutil.reference.fasta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class SingleChromosomeGenomeSequenceAccessorTest {

    private static final Path FASTA = Paths.get(SingleChromosomeGenomeSequenceAccessor.class.getResource("small_hg19.fa").getPath());
    private static final Path FASTA_FAI = Paths.get(SingleChromosomeGenomeSequenceAccessor.class.getResource("small_hg19.fa.fai").getPath());
    private static final Path FASTA_DICT = Paths.get(SingleChromosomeGenomeSequenceAccessor.class.getResource("small_hg19.fa.dict").getPath());
    private SingleChromosomeGenomeSequenceAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = new SingleChromosomeGenomeSequenceAccessor(FASTA, FASTA_FAI, FASTA_DICT);
    }

    @AfterEach
    void tearDown() throws Exception {
        accessor.close();
    }

    @Test
    void fetchSequence() {
        String seq = accessor.fetchSequence("chr1", 61, 70);
        assertThat(seq, is("caatgagccc"));

        seq = accessor.fetchSequence("chr2", 61, 70);
        assertThat(seq, is("TCTGCTGTGT"));
    }

    @Test
    void testConcurrency() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        final int count = 1_000;
        final CountDownLatch latch = new CountDownLatch(3);
        List<String> first = new ArrayList<>(count), second = new ArrayList<>(count), third = new ArrayList<>(count);
        executor.submit(() -> {
            for (int i = 0; i < count; i++) {
                final String seq = accessor.fetchSequence("chr1", 61, 70);
                first.add(seq);
            }
            latch.countDown();
        });
        executor.submit(() -> {
            for (int i = 0; i < count; i++) {
                final String seq = accessor.fetchSequence("chr2", 61, 70);
                second.add(seq);
            }
            latch.countDown();
        });
        executor.submit(() -> {
            for (int i = 0; i < count; i++) {
                final String seq = accessor.fetchSequence("chrM", 61, 70);
                third.add(seq);
            }
            latch.countDown();
        });
        latch.await();

        assertThat(first.stream().allMatch(s -> s.equals("caatgagccc")), is(true));
        assertThat(second.stream().allMatch(s -> s.equals("TCTGCTGTGT")), is(true));
        assertThat(third.stream().allMatch(s -> s.equals("CGTCTGGGGG")), is(true));
    }
}