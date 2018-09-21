package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;

/**
 * Generates features over a given predicate.
 *
 * @param <F> Feature type to be generated
 */
@FunctionalInterface
public interface PredicateFeatureGenerating<F extends Features>
        extends FeatureGenerating<F, BPredicate> {

    F generate(BPredicate predicate, MachineAccess bMachine) throws FeatureCreationException;

    @Override
    default F generate(BPredicate predicate) throws FeatureCreationException {
        return generate(predicate, null);
    }

    default F generate(String predicate, MachineAccess bMachine) throws FeatureCreationException {
        return generate(new BPredicate(predicate), bMachine);
    }

    default F generate(String predicate) throws FeatureCreationException {
        return generate(new BPredicate(predicate));
    }


}
