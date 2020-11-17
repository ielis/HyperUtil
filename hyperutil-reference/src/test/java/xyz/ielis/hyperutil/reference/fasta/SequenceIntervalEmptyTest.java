package xyz.ielis.hyperutil.reference.fasta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SequenceIntervalEmptyTest {

    private SequenceInterval instance;

    @BeforeEach
    public void setUp() {
        instance = SequenceInterval.empty();
    }

    @Test
    public void properties() {
        assertThat(instance.getInterval(), is(nullValue()));
        assertThat(instance.getSequence(), is(""));
        assertThat(instance.getSubsequence(null), is(Optional.empty()));
        assertThat(instance.toString(), is("EMPTY_SEQ"));
    }

    @Test
    public void emptyEquals() {
        assertThat(instance, is(equalTo(SequenceInterval.empty())));
        assertThat(instance, is(sameInstance(SequenceInterval.empty())));
    }

    @Test
    public void isEmpty() {
        assertThat(instance.isEmpty(), is(true));
    }
}