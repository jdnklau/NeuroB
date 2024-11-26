package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.Features;

import java.util.Objects;

/**
 * Features over B-predicates.
 */
public class PredicateFeatures extends Features {

    final BPredicate predicate;

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

            boolean arePredicatesEqual = Objects.equals(this.predicate, other.predicate);

            return arePredicatesEqual && super.equals(other);
        }

        return false;
    }
}
