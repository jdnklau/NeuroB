package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.api.data.BData;
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
        <D extends BData, L extends Labelling> {
    private final Path sourceFile;
    private final Stream<TrainingSample<D, L>> samples;

    public TrainingData(Path sourceFile, Stream<TrainingSample<D, L>> samples) {
        this.sourceFile = sourceFile;
        this.samples = samples;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public Stream<TrainingSample<D, L>> getSamples() {
        return samples;
    }
}
