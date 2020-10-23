package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.Features;

/**
 * Features of raw predicates.
 */
public class RawPredFeature extends PredicateFeatures {
    private final BPredicate pred;

    public RawPredFeature(String pred) {
        this(BPredicate.of(pred));
    }

    public RawPredFeature(BPredicate pred) {
        super(vectorizePred(pred));
        this.pred = pred;
    }

    private static Double[] vectorizePred(BPredicate pred) {
        String raw = pred.getPredicate().trim();
        Double[] vec = new Double[raw.length()];

        for (int i = 0; i < raw.length(); i++) {
            vec[i] = (double) raw.charAt(i);
        }

        return vec;
    }

    public BPredicate getPred() {
        return pred;
    }
}
