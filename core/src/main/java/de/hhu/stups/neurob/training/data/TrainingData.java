package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Data class connecting a B machine and a stream of its associated training
 * samples.
 *
 * @param <D> Type of encapsulated data.
 * @param <L> Labelling type used.
 */
public class TrainingData
        <D, L extends Labelling> {
    private final Path sourceFile;
    /** Abosulte path to the source file */
    private final Path absoluteSourcePath;

    private final Stream<TrainingSample<D, L>> samples;

    public TrainingData(Path sourceFile, Stream<TrainingSample<D, L>> samples) {
        this(sourceFile, sourceFile, samples);
    }

    /**
     * Initiates training data with a relative and and absolute path to the source file.
     * <p>
     * For example, the relative source can be "short/path/machine.mch"
     * whereas the absolute path is "/absolute/path/to/short/path/machine.mch".
     *
     * @param relativeSource
     * @param absoluteSource
     * @param samples
     */
    public TrainingData(Path relativeSource, Path absoluteSource, Stream<TrainingSample<D, L>> samples) {
        this.sourceFile = relativeSource;
        this.samples = samples;
        this.absoluteSourcePath = absoluteSource;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public Path getAbsoluteSourcePath() {
        return absoluteSourcePath;
    }

    public Stream<TrainingSample<D, L>> getSamples() {
        return samples;
    }

    /**
     * Splits this training data into two.
     * <p>
     * The training samples are randomly split to achieve the specified {@code ratio}.
     * The ratio must be a value between 0.0 and 1.0.
     * <p>
     * The returned array contains two TrainingData instances.
     * The index position 0 hereby contains samples according to the specified ratio,
     * whereas the index position 1 contains the remaining samples.
     * <p>
     * Calling {@link #getSamples()} afterwards in this instance will return a used stream.
     *
     * @param ratio Ratio of samples achieved by the split
     *
     * @return Array with two entries
     *
     * @see #split(Double, int)
     */
    public TrainingData<D, L>[] split(Double ratio) {
        return split(ratio, new Random().nextInt());
    }

    /**
     * Splits this training data into two.
     * <p>
     * The training samples are randomly split to achieve the specified {@code ratio}.
     * The ratio must be a value between 0.0 and 1.0.
     * <p>
     * The returned array contains two TrainingData instances.
     * The index position 0 hereby contains samples according to the specified ratio,
     * whereas the index position 1 contains the remaining samples.
     * <p>
     * Calling {@link #getSamples()} afterwards in this instance will return a used stream.
     *
     * @param ratio Ratio of samples achieved by the split
     * @param seed Seed for the RNG
     *
     * @return Array with two entries
     */
    public TrainingData<D, L>[] split(Double ratio, int seed) {
        return split(ratio, new Random(seed));
    }

    /**
     * Splits this training data into two.
     * <p>
     * The training samples are randomly split to achieve the specified {@code ratio}.
     * The ratio must be a value between 0.0 and 1.0.
     * <p>
     * The returned array contains two TrainingData instances.
     * The index position 0 hereby contains samples according to the specified ratio,
     * whereas the index position 1 contains the remaining samples.
     * <p>
     * Calling {@link #getSamples()} afterwards in this instance will return a used stream.
     *
     * @param ratio Ratio of samples achieved by the split
     * @param rng The {@link Random} object to be used
     *
     * @return Array with two entries
     */
    public TrainingData<D, L>[] split(Double ratio, Random rng) {
        if (ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("Ratio for splitting must be between 0 and 1.");
        }

        Map<Boolean, List<TrainingSample<D, L>>> collect =
                samples.collect(Collectors.partitioningBy(s -> rng.nextDouble() <= ratio));

        TrainingData<D, L> first = new TrainingData<>(sourceFile, absoluteSourcePath, collect.get(true).stream());
        TrainingData<D, L> second = new TrainingData<>(sourceFile, absoluteSourcePath, collect.get(false).stream());

        TrainingData<D, L>[] split = new TrainingData[]{first, second};
        return split;
    }
}
