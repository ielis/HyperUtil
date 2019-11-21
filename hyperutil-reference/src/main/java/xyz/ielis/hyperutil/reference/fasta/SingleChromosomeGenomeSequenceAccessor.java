package xyz.ielis.hyperutil.reference.fasta;

import htsjdk.samtools.reference.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * This accessor will hold sequence of a whole single chromosome in memory and thus it will be much quicker serving
 * queries sequentially asking for sequences from a single chromosome.
 * <p>
 * However, it will be dramatically slower when asking for sequences from different chromosomes.
 * </p>
 * <p>
 * This class is thread-safe.
 * </p>
 */
public class SingleChromosomeGenomeSequenceAccessor extends SingleFastaGenomeSequenceAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleChromosomeGenomeSequenceAccessor.class);


    private ReferenceSequence referenceSequence = null;

    SingleChromosomeGenomeSequenceAccessor(Path fastaPath) {
        super(fastaPath);
    }

    SingleChromosomeGenomeSequenceAccessor(Path fastaPath, Path fastaFai, Path fastaDict) {
        super(fastaPath, fastaFai, fastaDict);
    }

    @Override
    public synchronized String fetchSequence(String chromosome, int begin, int end) {
        if (referenceSequence == null || !referenceSequence.getName().equals(chromosome)) {
            // the query does not ask for a string from the current referenceSequence, we need to load it into memory
            referenceSequence = fasta.getSequence(chromosome);
        }
        return referenceSequence.getBaseString().substring(begin - 1, end);
    }

}

