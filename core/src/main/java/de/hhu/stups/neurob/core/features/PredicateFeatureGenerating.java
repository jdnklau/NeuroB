package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.prob.statespace.StateSpace;

/**
 * Generates features over a given predicate.
 *
 * @param <F> Feature type to be generated
 */
@FunctionalInterface
public interface PredicateFeatureGenerating<F extends Features>
        extends FeatureGenerating<F, String> {

    F generate(String predicate, StateSpace ss) throws FeatureCreationException;

    @Override
    default F generate(String predicate) throws FeatureCreationException {
        return generate(predicate, null);
    }

}
