package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;

/**
 * Functional interface for a feature generation step.
 * <p>
 * Implementing classes provide a way to generate a feature given a single
 * input.
 * </p>
 *
 * @param <F> Feature type being generated
 * @param <In> Input type
 */
@FunctionalInterface
public interface FeatureGenerating<F extends Features, In> {

    F generate(In source) throws FeatureCreationException;

}
