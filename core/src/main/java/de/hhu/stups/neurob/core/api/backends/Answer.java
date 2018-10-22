package de.hhu.stups.neurob.core.api.backends;

/**
 * Enum over possible answers yielded during solving of a predicate.
 */
public enum Answer {
    /** Predicate has a solution */
    VALID("VALID"),
    /** Predicate has no solution */
    INVALID("INVALID"),
    /** Predicate is not solvable, neither does a counter example exist */
    UNKNOWN("UNKNOWN"),
    /** Predicate was not solvable within a specified timeout */
    TIMEOUT("TIMEOUT"),
    /** Predicate led to an error and thus was not solvable */
    ERROR("ERROR");

    private final String code;

    Answer(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
