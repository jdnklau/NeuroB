package de.hhu.stups.neurob.core.labelling;

/**
 * Labelling of B-Predicates.
 */
public class PredicateLabelling extends Labelling {

    protected final String predicate;

    public PredicateLabelling(String predicate, Double... labellingArray) {
        super(labellingArray);
        this.predicate = (predicate != null) ? predicate : "";
    }

    /**
     * @return The predicate that was labelled.
     */
    public String getPredicate() {
        return predicate;
    }
}
