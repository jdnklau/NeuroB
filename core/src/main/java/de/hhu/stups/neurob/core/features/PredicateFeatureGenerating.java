package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;

/**
 * Generates features over a given predicate.
 *
 * @param <F> Feature type to be generated
 */
@FunctionalInterface
public interface PredicateFeatureGenerating<F>
        extends FeatureGenerating<F, BPredicate> {

    F generate(BPredicate predicate, BMachine bMachine) throws FeatureCreationException;

    @Override
    default F generate(BPredicate predicate) throws FeatureCreationException {
        return generate(predicate, null);
    }

    default F generate(String predicate, BMachine bMachine) throws FeatureCreationException {
        return generate(new BPredicate(predicate), bMachine);
    }

    default F generate(String predicate) throws FeatureCreationException {
        return generate(new BPredicate(predicate));
    }


}
