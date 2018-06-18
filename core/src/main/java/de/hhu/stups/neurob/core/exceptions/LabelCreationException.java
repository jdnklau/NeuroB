package de.hhu.stups.neurob.core.exceptions;

/**
 * Indicates an exception caused by trying to determine a labelling.
 */
public class LabelCreationException extends Exception {

    public LabelCreationException() {
        super();
    }

    public LabelCreationException(String message) {
        super(message);
    }

    public LabelCreationException(Throwable cause) {
        super(cause);
    }

    public LabelCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
