package de.hhu.stups.neurob.core.api.bmethod;

public class BPredicate implements BElement{

    private final String predicate;

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
    public boolean equals(Object o) {
        if(o instanceof BPredicate) {
            return predicate.equals(((BPredicate) o).predicate);
        }
        return false;
    }
}
