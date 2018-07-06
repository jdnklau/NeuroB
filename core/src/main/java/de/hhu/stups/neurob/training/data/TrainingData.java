package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Data class connecting a B machine and a stream of its associated training
 * samples.
 *
 * @param <F>
 * @param <L>
 */
public class TrainingData
        <F extends Features, L extends Labelling> {
    private final Path sourceFile;
    private final Stream<TrainingSample<F, L>> samples;

    public TrainingData(Path sourceFile, Stream<TrainingSample<F, L>> samples) {
        this.sourceFile = sourceFile;
        this.samples = samples;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public Stream<TrainingSample<F, L>> getSamples() {
        return samples;
    }
}
