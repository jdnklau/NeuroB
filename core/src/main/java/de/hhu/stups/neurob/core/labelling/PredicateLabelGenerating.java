package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.prob.statespace.StateSpace;

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
     * Generate the labelling of the Predicate with help of the given {@link
     * StateSpace}.
     *
     * @param predicate
     * @param ss
     *
     * @return
     */
    L generate(String predicate, StateSpace ss) throws LabelCreationException;
}
