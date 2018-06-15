package de.hhu.stups.neurob.core.exceptions;

/**
 * Indicates an error during access of a B machine.
 */
public class MachineAccessException extends Exception{
    public MachineAccessException() {
    }

    public MachineAccessException(String message) {
        super(message);
    }

    public MachineAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public MachineAccessException(Throwable cause) {
        super(cause);
    }
}
