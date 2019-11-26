package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.data.ReferenceDictionaryBuilder;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceIntervalTest {

    private static ReferenceDictionary RD;

    private SequenceInterval si;

    @BeforeAll
    static void setUpBefore() {
        ReferenceDictionaryBuilder rdb = new ReferenceDictionaryBuilder();
        rdb.putContigName(1, "chr1");
        rdb.putContigID("chr1", 1);
        rdb.putContigLength(1, 10_000);

        rdb.putContigName(2, "chr2");
        rdb.putContigID("chr2", 2);
        rdb.putContigLength(2, 20_000);

        RD = rdb.build();
    }

    @BeforeEach
    void setUp() {
        si = SequenceInterval.builder()
                .interval(new GenomeInterval(RD, Strand.FWD, 1, 10, 20))
                .sequence("ACGTACGTAC")
                .build();
    }

    @Test
    void getInterval() {
        assertThat(si.getInterval(), is(new GenomeInterval(RD, Strand.FWD, 1, 10, 20)));
    }

    @Test
    void getSequence() {
        assertThat(si.getSequence(), is("ACGTACGTAC"));
    }

    @Test
    void getSubsequence() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        assertThat(si.getSubsequence(interval), is(Optional.of("ACGTACGTAC")));
        assertThat(si.getSubsequence(interval.withStrand(Strand.REV)), is(Optional.of("GTACGTACGT")));
    }

    @Test
    void getEmptySubsequence() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 10, 10);
        assertThat(si.getSubsequence(interval), is(Optional.of("")));

        interval = new GenomeInterval(RD, Strand.FWD, 1, 20, 20);
        assertThat(si.getSubsequence(interval), is(Optional.of("")));
    }

    @Test
    void queryOutsideRange() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 9, 10);
        assertThat(si.getSubsequence(interval), is(Optional.empty()));

        interval = new GenomeInterval(RD, Strand.FWD, 1, 20, 21);
        assertThat(si.getSubsequence(interval), is(Optional.empty()));
    }

    @Test
    void intervalLengthDoesNotMatchSequenceLength() {
        assertThrows(IllegalArgumentException.class,
                () -> SequenceInterval.builder()
                        .sequence("AC")  // length 2
                        .interval(new GenomeInterval(RD, Strand.FWD, 1, 10, 11)) // length 1
                        .build());
    }

    @Test
    void illegalBasePresent() {
        assertThrows(IllegalArgumentException.class,
                () -> SequenceInterval.builder()
                        .sequence("ACGX")  // X is not permitted
                        .interval(new GenomeInterval(RD, Strand.FWD, 1, 10, 14))
                        .build());
    }

    @Test
    void isEqualTo() {
        GenomeInterval first = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        GenomeInterval second = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        SequenceInterval firstSi = SequenceInterval.builder()
                .interval(first)
                .sequence("ACGTACGTAC")
                .build();
        SequenceInterval secondSi = SequenceInterval.builder().interval(second).sequence("ACGTACGTAC").build();
        assertThat(firstSi, is(equalTo(secondSi)));
    }

}