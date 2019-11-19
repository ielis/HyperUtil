package xyz.ielis.hyperutil.reference.fasta;


/**
 *
 */
public class InvalidFastaFileException extends RuntimeException {

    public InvalidFastaFileException() {
        super();
    }

    public InvalidFastaFileException(String message) {
        super(message);
    }

    public InvalidFastaFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFastaFileException(Throwable cause) {
        super(cause);
    }

    protected InvalidFastaFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
