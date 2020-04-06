package de.hhu.stups.neurob.core.exceptions;

/**
 * Indicates an error during handling of a formula.
 */
public class FormulaException extends Exception{
    public FormulaException() {
    }

    public FormulaException(String message) {
        super(message);
    }

    public FormulaException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaException(Throwable cause) {
        super(cause);
    }
}
