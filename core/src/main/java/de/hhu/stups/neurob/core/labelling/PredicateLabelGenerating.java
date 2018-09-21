package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;

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
        return generate(predicate, null);
    }

    default L generate(String predicate) throws LabelCreationException {
        return generate(BPredicate.of(predicate), null);
    }

    default L generate(String predicate, MachineAccess bMachine) throws LabelCreationException {
        return generate(BPredicate.of(predicate), bMachine);
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
