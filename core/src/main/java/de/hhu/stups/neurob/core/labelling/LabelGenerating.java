package de.hhu.stups.neurob.core.labelling;

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
    L generate(In source);
}
