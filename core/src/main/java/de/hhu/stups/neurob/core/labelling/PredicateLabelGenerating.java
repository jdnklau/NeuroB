package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;

/**
 * Generates a labelling for a given predicate.
 *
 * @param <L> Labelling class to be generated.
 */
@FunctionalInterface
public interface PredicateLabelGenerating<L extends PredicateLabelling>
        extends LabelGenerating<L, String> {
    @Override
    default L generate(String predicate) throws LabelCreationException {
        return generate(predicate, null);
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
    L generate(String predicate, MachineAccess bMachine) throws LabelCreationException;
}
