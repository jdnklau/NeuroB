package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.api.Labelling;
import de.hhu.stups.neurob.training.data.TrainingSample;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface TrainingDataFormat {

    /**
     * Generates the target location path to be written to at training data
     * generation time.
     *
     * @param sourceFile
     * @param targetDirectory
     *
     * @return Path of the target for training data generation
     */
    Path getTargetLocation(Path sourceFile, Path targetDirectory);

    /**
     * Writes the given <code>samples</code> into the target location in the
     * respective format.
     *
     * @param samples List of training data samples
     * @param targetDirectory location to populate with training data
     */
    default
    <F extends Features, L extends Labelling>
    void writeSamples(List<TrainingSample<F, L>> samples,
            Path targetDirectory) {
        writeSamples(samples.stream(), targetDirectory);
    }

    /**
     * Writes the given <code>samples</code> into the target location in the
     * respective format.
     *
     * @param samples Stream of training data samples
     * @param targetDirectory location to populate with training data
     */
    <F extends Features, L extends Labelling>
    void writeSamples(Stream<TrainingSample<F, L>> samples,
            Path targetDirectory);

}
