package de.hhu.stups.neurob.core.features;

/**
 * Features over B-predicates.
 */
public abstract class PredicateFeatures implements Features {

    protected final String predicate;

    public PredicateFeatures(String predicate) {
        this.predicate = predicate;
    }

    public String getPredicate() {
        return predicate;
    }
}
