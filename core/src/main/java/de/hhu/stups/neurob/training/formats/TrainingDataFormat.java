package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface TrainingDataFormat<F extends Features> {

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
     * Writes the given <code>trainingData</code> into the target location
     * in the respective format.
     *
     * @param trainingData
     * @param targetDirectory Location to populate with training data
     */
    <L extends Labelling>
    void writeSamples(TrainingData<F, L> trainingData,
            Path targetDirectory);

}
