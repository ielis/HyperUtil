package xyz.ielis.hyperutil.reference.fasta;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomeInterval;

import java.io.Closeable;

public interface GenomeSequenceAccessor extends Closeable {

    ReferenceDictionary getReferenceDictionary();

    String fetchSequence(String chromosome, int begin, int end);

    String fetchSequence(GenomeInterval interval);
}
