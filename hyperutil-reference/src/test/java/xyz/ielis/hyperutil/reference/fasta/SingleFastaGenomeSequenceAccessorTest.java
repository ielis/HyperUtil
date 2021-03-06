package xyz.ielis.hyperutil.reference.fasta;


import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class SingleFastaGenomeSequenceAccessorTest {

    private static final Path FASTA = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa").getPath());
    private static final Path FASTA_FAI = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa.fai").getPath());
    private static final Path FASTA_DICT = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19.fa.dict").getPath());

    private static final Path FASTA_2 = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19_2.fa").getPath());
    private static final Path FASTA_2_FAI = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19_2.fa.fai").getPath());
    private static final Path FASTA_2_DICT = Paths.get(SingleFastaGenomeSequenceAccessorTest.class.getResource("small_hg19_2.fa.dict").getPath());

    private SingleFastaGenomeSequenceAccessor accessor;

    @BeforeEach
    public void setUp() {
        accessor = new SingleFastaGenomeSequenceAccessor(FASTA, FASTA_FAI, FASTA_DICT, true);
    }

    @AfterEach
    public void tearDown() throws Exception {
        accessor.close();
    }

    @Test
    public void fetchSequence() {
        String seq = accessor.fetchSequence("chr1", 61, 70);
        assertThat(seq, is("caatgagccc"));

        seq = accessor.fetchSequence("chr2", 61, 70);
        assertThat(seq, is("TCTGCTGTGT"));
    }

    @Test
    public void fetchSequenceForGenomeInterval() {
        ReferenceDictionary rd = accessor.getReferenceDictionary();
        GenomeInterval query = new GenomeInterval(rd, Strand.FWD, 0, 60, 70);

        Optional<SequenceInterval> seqOpt = accessor.fetchSequence(query);
        assertThat(seqOpt.isPresent(), is(true));
        SequenceInterval seq = seqOpt.get();

        assertThat(seq.getInterval(), is(equalTo(query)));
        assertThat(seq.getSequence(), is("caatgagccc"));

        seqOpt = accessor.fetchSequence(query.withStrand(Strand.REV)); // now try to fetch reverse complement
        assertThat(seqOpt.isPresent(), is(true));
        seq = seqOpt.get();

        assertThat(seq.getInterval(), is(equalTo(query.withStrand(Strand.REV))));
        assertThat(seq.getSequence(), is("gggctcattg"));
    }

    @Test
    public void fetchSequenceFromUnknownContig() {
        ReferenceDictionary rd = accessor.getReferenceDictionary();
        GenomeInterval query = new GenomeInterval(rd, Strand.FWD, 100, 60, 70); // chr 100 is unknown
        Optional<SequenceInterval> seqOpt = accessor.fetchSequence(query);
        assertThat(seqOpt.isPresent(), is(false));
    }

    @Test
    public void getReferenceDictionary() {
        final ReferenceDictionary rd = accessor.getReferenceDictionary();
        final ImmutableMap<String, Integer> contigNameToID = rd.getContigNameToID();
        assertThat(contigNameToID.keySet(), hasSize(8));
        assertThat(contigNameToID.keySet(), hasItems("chr1", "1", "chr2", "2", "chrM", "M", "chrMT", "MT"));

        final ImmutableMap<Integer, String> contigIDToName = rd.getContigIDToName();
        assertThat(contigIDToName.keySet(), hasSize(3));
        assertThat(contigIDToName.keySet(), hasItems(0, 1, 2));

        assertThat(contigIDToName.values(), hasSize(3));
        assertThat(contigIDToName.values(), hasItems("chr1", "chr2", "chrM"));


        final ImmutableMap<Integer, Integer> contigIDToLength = rd.getContigIDToLength();
        assertThat(contigIDToLength.keySet(), hasSize(3));
        assertThat(contigIDToLength.keySet(), hasItems(0, 1, 2));
        assertThat(contigIDToLength.values(), hasSize(3));
        assertThat(contigIDToLength.values(), hasItems(10_001, 10_001, 1000));
    }

    @Test
    public void returnsEmptyWhenAskingForSequencePastEndOfTheContig() {
        final Optional<SequenceInterval> opt = accessor.fetchSequence(new GenomeInterval(accessor.getReferenceDictionary(), Strand.FWD, 0, 9_000, 10_002));
        assertThat(opt.isEmpty(), is(true));
    }

    @Test
    public void usesPrefix() {
        // the test reference genome uses prefixes, hence we require prefixed contig names when accessing by ID
        final ReferenceDictionary rd = accessor.getReferenceDictionary();
        assertThat(rd.getContigIDToName().get(0), is("chr1"));
        assertThat(rd.getContigIDToName().get(1), is("chr2"));
        assertThat(rd.getContigIDToName().get(2), is("chrM"));
    }

    @Test
    public void failsWhenMitochondrialChromosomeIsMissing() {
        assertThrows(InvalidFastaFileException.class, () -> new SingleFastaGenomeSequenceAccessor(FASTA_2, FASTA_2_FAI, FASTA_2_DICT));
    }

    @Test
    public void worksWhenMitochondrialIsMissing() {
        SingleFastaGenomeSequenceAccessor accessor = new SingleFastaGenomeSequenceAccessor(FASTA_2, FASTA_2_FAI, FASTA_2_DICT, false);
        assertThat(accessor.getReferenceDictionary().getContigNameToID().keySet(), hasItems("chr1", "1", "chr2", "2"));
    }
}