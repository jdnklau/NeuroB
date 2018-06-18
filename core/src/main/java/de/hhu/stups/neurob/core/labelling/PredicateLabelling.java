package de.hhu.stups.neurob.core.labelling;

/**
 * Labelling of B-Predicates.
 */
public interface PredicateLabelling extends Labelling{
    /**
     * @return The predicate that was labelled.
     */
    String getPredicate();
}
