package de.hhu.stups.neurob.core.api.bmethod;

import org.jetbrains.annotations.NotNull;

public class BPredicate implements BElement, Comparable<BPredicate> {

    private final String predicate;

    /**
     * Instantiates a new predicate form the given String.
     *
     * @param rawString
     *
     * @return
     */
    public static BPredicate of(String rawString) {
        return new BPredicate(rawString);
    }

    public BPredicate(String predicate) {
        this.predicate = (predicate != null) ? predicate : "";
    }

    public String getPredicate() {
        return predicate;
    }

    @Override
    public String toString() {
        return predicate;
    }

    @Override
    public int hashCode() {
        if (predicate == null) {
            return 0;
        }
        return predicate.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BPredicate) {
            return predicate.equals(((BPredicate) o).predicate);
        }
        // Allow direct comparison with strings
        if (o instanceof String) {
            return o.equals(predicate);
        }
        return false;
    }

    @Override
    public int compareTo(@NotNull BPredicate other) {
        return predicate.compareTo(other.predicate);
    }
}
