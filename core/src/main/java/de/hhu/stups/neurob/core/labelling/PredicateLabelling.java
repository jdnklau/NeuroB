package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;

/**
 * Labelling of B-Predicates.
 */
public class PredicateLabelling extends Labelling {

    protected final BPredicate predicate;

    public PredicateLabelling(String predicate, Double... labellingArray) {
        this(BPredicate.of(predicate), labellingArray);
    }

    public PredicateLabelling(BPredicate predicate, Double... labellingArray) {
        super(labellingArray);
        this.predicate = predicate;
    }
    /**
     * @return The predicate that was labelled.
     */
    public BPredicate getPredicate() {
        return predicate;
    }
}
