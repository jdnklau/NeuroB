package de.hhu.stups.neurob.core.exceptions;

/**
 * Indicates an exception caused by trying to instantiate features.
 */
public class FeatureCreationException extends Exception {

    public FeatureCreationException() {
        super();
    }

    public FeatureCreationException(String message) {
        super(message);
    }

    public FeatureCreationException(Throwable cause) {
        super(cause);
    }

    public FeatureCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
