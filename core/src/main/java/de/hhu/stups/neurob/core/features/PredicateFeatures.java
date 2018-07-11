package de.hhu.stups.neurob.core.features;

/**
 * Features over B-predicates.
 */
public class PredicateFeatures extends Features {

    private final String predicate;

    public PredicateFeatures(String predicate, Double... features) {
        super(features);
        this.predicate = predicate;
    }

    public PredicateFeatures(Double... features) {
        this(null, features);
    }

    public String getPredicate() {
        return predicate;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PredicateFeatures) {
            PredicateFeatures other = (PredicateFeatures) o;

            boolean arePredicatesEqual = (this.predicate == null)
                    ? other.predicate == null
                    : this.predicate.equals(other.predicate);

            return arePredicatesEqual && super.equals(other);
        }

        return false;
    }
}
