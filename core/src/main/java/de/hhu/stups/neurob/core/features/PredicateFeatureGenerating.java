package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;

/**
 * Generates features over a given predicate.
 *
 * @param <F> Feature type to be generated
 */
@FunctionalInterface
public interface PredicateFeatureGenerating<F extends Features>
        extends FeatureGenerating<F, String> {

    F generate(String predicate, MachineAccess bMachine) throws FeatureCreationException;

    @Override
    default F generate(String predicate) throws FeatureCreationException {
        return generate(predicate, null);
    }

}
