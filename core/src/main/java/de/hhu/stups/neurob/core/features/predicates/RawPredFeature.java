package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.Features;

import javax.annotation.Nullable;

/**
 * Features of raw predicates.
 */
public class RawPredFeature extends PredicateFeatures {

    public RawPredFeature(String pred) {
        this(BPredicate.of(pred));
    }

    public RawPredFeature(BPredicate pred) {
        super(pred, vectorizePred(pred));
    }

    private static Double[] vectorizePred(BPredicate pred) {
        String raw = pred.getPredicate().trim();
        Double[] vec = new Double[raw.length()];

        for (int i = 0; i < raw.length(); i++) {
            vec[i] = (double) raw.charAt(i);
        }

        return vec;
    }

    public static class Generator implements PredicateFeatureGenerating<RawPredFeature> {

        @Override
        public RawPredFeature generate(BPredicate predicate, @Nullable MachineAccess machineAccess) throws FeatureCreationException {
            return new RawPredFeature(predicate);
        }
    }
}
