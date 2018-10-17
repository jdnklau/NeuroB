package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;

import javax.annotation.Nullable;

/**
 * Generates features over a given predicate.
 *
 * @param <F> Feature type to be generated
 */
@FunctionalInterface
public interface PredicateFeatureGenerating<F>
        extends FeatureGenerating<F, BPredicate> {

    F generate(BPredicate predicate, @Nullable MachineAccess machineAccess) throws FeatureCreationException;

    default F generate(BPredicate predicate, BMachine bMachine) throws FeatureCreationException {
        F features;
        try {
            MachineAccess access = (bMachine != null) ? bMachine.getMachineAccess() : null;
            features = generate(predicate, access);
            if(access != null) {
                bMachine.closeMachineAccess();
            }
        } catch (MachineAccessException e) {
            throw new FeatureCreationException("Could not create features for " + predicate, e);
        }
        return features;
    }

    @Override
    default F generate(BPredicate predicate) throws FeatureCreationException {
        return generate(predicate, (MachineAccess) null);
    }

    default F generate(String predicate, BMachine bMachine) throws FeatureCreationException {
        return generate(new BPredicate(predicate), bMachine);
    }

    default F generate(String predicate) throws FeatureCreationException {
        return generate(new BPredicate(predicate));
    }


}
