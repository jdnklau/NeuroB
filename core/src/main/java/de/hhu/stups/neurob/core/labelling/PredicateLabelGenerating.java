package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;

import java.util.ArrayList;
import java.util.List;

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
            if (access != null) {
                access.close();
            }
        } catch (MachineAccessException e) {
            throw new LabelCreationException("Could not create features for " + predicate, e);
        }
        return labelling;
    }

    /**
     * Calls {@link #generate(BPredicate, MachineAccess)})} consecutively for the specified amount
     * of times, returning the labels in a list.
     * <p>
     * This is meant for cases in which the labelling might slightly differ
     * from call to call, allowing to conveniently gather multiple values,
     * e.g. in a regression over runtime.
     *
     * @param predicate
     *
     * @return
     */
    default List<L> generateSamples(BPredicate predicate, int sampleSize) throws LabelCreationException {
        return generateSamples(predicate, (MachineAccess) null, sampleSize);
    }

    /**
     * Calls {@link #generate(BPredicate, MachineAccess)})} consecutively for the specified amount
     * of times, returning the labels in a list.
     * <p>
     * This is meant for cases in which the labelling might slightly differ
     * from call to call, allowing to conveniently gather multiple values,
     * e.g. in a regression over runtime.
     *
     * @param predicate
     * @param bMachine
     *
     * @return
     */
    default List<L> generateSamples(BPredicate predicate, BMachine bMachine, int sampleSize)
            throws LabelCreationException {
        List<L> labelling;
        try {
            MachineAccess access = (bMachine != null) ? bMachine.spawnMachineAccess() : null;
            labelling = generateSamples(predicate, access, sampleSize);
            if (access != null) {
                access.close();
            }
        } catch (MachineAccessException e) {
            throw new LabelCreationException("Could not create features for " + predicate, e);
        }
        return labelling;
    }

    /**
     * Calls {@link #generate(BPredicate, MachineAccess)})} consecutively for the specified amount
     * of times, returning the labels in a list.
     * <p>
     * This is meant for cases in which the labelling might slightly differ
     * from call to call, allowing to conveniently gather multiple values,
     * e.g. in a regression over runtime.
     *
     * @param predicate
     * @param access
     *
     * @return
     */
    default List<L> generateSamples(BPredicate predicate, MachineAccess access, int sampleSize)
            throws LabelCreationException {
        List<L> results = new ArrayList<>();

        for (int i = 0; i < sampleSize; i++) {
            results.add(generate(predicate, access));
        }

        return results;
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
