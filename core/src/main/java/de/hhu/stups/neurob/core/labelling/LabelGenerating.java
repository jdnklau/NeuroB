package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Functional interface for a labelling step.
 * <p>
 * Implementing classes provide a way to generate a labelling given a single
 * input.
 * </p>
 *
 * @param <L> {@link Labelling} to be created.
 * @param <In> Type of input to be labelled.
 */
@FunctionalInterface
public interface LabelGenerating<L extends Labelling, In> {

    L generate(In source) throws LabelCreationException;

    /**
     * Calls {@link #generate(Object)} consecutively for the specified amount
     * of times, returning the labels in a list.
     *
     * This is meant for cases in which the labelling might slightly differ
     * from call to call, allowing to conveniently gather multiple values,
     * e.g. in a regression over runtime.
     *
     * @param source
     * @param sampleSize
     * @return
     */
    default List<L> generateSamples(In source, int sampleSize) throws LabelCreationException {
        List<L> results = new ArrayList<>();

        for (int i = 0; i< sampleSize; i++) {
            // Logging?
            results.add(generate(source));
        }

        return results;
    }
}
