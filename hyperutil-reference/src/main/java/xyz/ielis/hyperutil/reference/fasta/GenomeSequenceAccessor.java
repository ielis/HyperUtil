package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.io.Closeable;
import java.util.Optional;

public interface GenomeSequenceAccessor extends Closeable {

    ReferenceDictionary getReferenceDictionary();

    String fetchSequence(String chromosome, int begin, int end);

    Optional<SequenceInterval> fetchSequence(GenomeInterval interval);

}
