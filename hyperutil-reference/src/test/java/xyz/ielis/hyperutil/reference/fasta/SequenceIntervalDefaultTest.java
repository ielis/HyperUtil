package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.data.ReferenceDictionaryBuilder;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SequenceIntervalDefaultTest {

    private static ReferenceDictionary RD;

    private SequenceIntervalDefault si;

    @BeforeAll
    public static void setUpBefore() {
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
    public void setUp() {
        si = SequenceIntervalDefault.of(new GenomeInterval(RD, Strand.FWD, 1, 10, 20), "ACGTACGTAC");
    }

    @Test
    public void getInterval() {
        assertThat(si.getInterval(), is(new GenomeInterval(RD, Strand.FWD, 1, 10, 20)));
    }

    @Test
    public void getSequence() {
        assertThat(si.getSequence(), is("ACGTACGTAC"));
    }

    @Test
    public void getSubsequence() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        assertThat(si.getSubsequence(interval), is(Optional.of("ACGTACGTAC")));
        assertThat(si.getSubsequence(interval.withStrand(Strand.REV)), is(Optional.of("GTACGTACGT")));
    }

    @Test
    public void getEmptySubsequence() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 10, 10);
        assertThat(si.getSubsequence(interval), is(Optional.of("")));

        interval = new GenomeInterval(RD, Strand.FWD, 1, 20, 20);
        assertThat(si.getSubsequence(interval), is(Optional.of("")));
    }

    @Test
    public void queryOutsideRange() {
        GenomeInterval interval = new GenomeInterval(RD, Strand.FWD, 1, 9, 10);
        assertThat(si.getSubsequence(interval), is(Optional.empty()));

        interval = new GenomeInterval(RD, Strand.FWD, 1, 20, 21);
        assertThat(si.getSubsequence(interval), is(Optional.empty()));
    }

    @Test
    public void intervalLengthDoesNotMatchSequenceLength() {
        assertThrows(IllegalArgumentException.class, () ->
                SequenceIntervalDefault.of(new GenomeInterval(RD, Strand.FWD, 1, 10, 11), "AC"));
    }

    @Test
    public void isEqualTo() {
        GenomeInterval first = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        GenomeInterval second = new GenomeInterval(RD, Strand.FWD, 1, 10, 20);
        SequenceInterval firstSi = SequenceIntervalDefault.of(first, "ACGTACGTAC");
        SequenceInterval secondSi = SequenceIntervalDefault.of(second, "ACGTACGTAC");
        assertThat(firstSi, is(equalTo(secondSi)));
    }

    @ParameterizedTest
    @CsvSource({"A,T", "C,G", "G,C", "T,A", "U,A", // individual conversions work
            "W,W", "S,S", "M,K", "K,M", "R,Y", "Y,R",
            "B,V", "D,H", "H,D", "V,B", "N,N",
            "AtcGuB,VaCgaT", // reordering works
            "ATCxX,NNGAT" // unknown bases are complementary to N
    })
    public void reverseComplement(String template, String expected) {
        assertThat(SequenceIntervalDefault.reverseComplement(template), is(expected));
    }

    @Test
    public void isEmpty() {
        assertThat(si.isEmpty(), is(false));
    }
}