package xyz.ielis.hyperutil.reference.fasta;


import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class SingleFastaGenomeSequenceAccessorTest {

    private static final Path FASTA = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa").getPath());
    private static final Path FASTA_FAI = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa.fai").getPath());
    private static final Path FASTA_DICT = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa.dict").getPath());

    private SingleFastaGenomeSequenceAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = new SingleFastaGenomeSequenceAccessor(FASTA, FASTA_FAI, FASTA_DICT);
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
    void fetchSequenceForGenomeInterval() {
        ReferenceDictionary rd = accessor.getReferenceDictionary();
        GenomeInterval query = new GenomeInterval(rd, Strand.FWD, 0, 60, 70);
        String seq = accessor.fetchSequence(query);
        assertThat(seq, is("caatgagccc"));

        seq = accessor.fetchSequence(query.withStrand(Strand.REV)); // now try to fetch reverse complement
        assertThat(seq, is("gggctcattg"));
    }
}