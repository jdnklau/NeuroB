package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;

/**
 * Features over B-predicates.
 */
public class PredicateFeatures extends Features {

    private final BPredicate predicate;

    public PredicateFeatures(String predicate, Double... features) {
        this(new BPredicate(predicate), features);
    }

    public PredicateFeatures(BPredicate predicate, Double... features) {
        super(features);
        this.predicate = predicate;
    }

    public PredicateFeatures(Double... features) {
        this((BPredicate) null, features);
    }

    public BPredicate getPredicate() {
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
