package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;
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
     *
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
}
