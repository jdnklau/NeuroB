package de.hhu.stups.neurob.core.labelling;

import javax.annotation.Nonnull;

/**
 * Labelling of B-Predicates.
 */
public class PredicateLabelling extends Labelling {

    protected final String predicate;

    public PredicateLabelling(@Nonnull String predicate, Double... labellingArray) {
        super(labellingArray);
        this.predicate = predicate;
    }

    /**
     * @return The predicate that was labelled.
     */
    public String getPredicate() {
        return predicate;
    }
}
