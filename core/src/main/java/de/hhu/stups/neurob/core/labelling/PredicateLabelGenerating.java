package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;

/**
 * Generates a labelling for a given predicate.
 *
 * @param <L> Labelling class to be generated.
 */
@FunctionalInterface
public interface PredicateLabelGenerating<L extends PredicateLabelling>
        extends LabelGenerating<L, BPredicate> {

    @Override
    default L generate(BPredicate predicate) throws LabelCreationException {
        return generate(predicate, (MachineAccess) null);
    }

    default L generate(String predicate) throws LabelCreationException {
        return generate(BPredicate.of(predicate), (MachineAccess) null);
    }

    default L generate(String predicate, BMachine bMachine) throws LabelCreationException {
        return generate(BPredicate.of(predicate), bMachine);
    }

    default L generate(BPredicate predicate, BMachine bMachine) throws LabelCreationException {
        L labelling;
        try {
            MachineAccess access = (bMachine != null) ? bMachine.spawnMachineAccess() : null;
            labelling = generate(predicate, access);
            if(access != null) {
                access.close();
            }
        } catch (MachineAccessException e) {
            throw new LabelCreationException("Could not create features for " + predicate, e);
        }
        return labelling;
    }

    /**
     * Generate the labelling of the Predicate over the given
     * {@link MachineAccess}.
     *
     * @param predicate
     * @param bMachine
     *
     * @return
     */
    L generate(BPredicate predicate, MachineAccess bMachine) throws LabelCreationException;
}
