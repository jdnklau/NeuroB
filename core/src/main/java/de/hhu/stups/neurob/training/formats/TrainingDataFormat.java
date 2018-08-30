package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;

import java.io.IOException;
import java.nio.file.Path;

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
    default Path getTargetLocation(Path sourceFile, Path targetDirectory) {
        // Get String representation
        String source;
        if (sourceFile == null) {
            source = "null." + getFileExtension();
        } else {
            source = sourceFile.toString();
        }

        // Replace extension
        int extPos = source.lastIndexOf('.');
        String target = source.substring(0, extPos +1 ) + getFileExtension();

        return targetDirectory.resolve(target);
    }

    /**
     * Writes the given <code>trainingData</code> into the target location
     * in the respective format.
     *
     * @param trainingData
     * @param targetDirectory Location to populate with training data
     */
    <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<F, L> trainingData,
            Path targetDirectory) throws IOException;

    /**
     * Returns the extension used for written files.
     *
     * If the extension for example would be "ext", generated files will
     * take names like "generated_file.ext".
     *
     * The returned extension is without the leading dot.
     * @return
     */
    String getFileExtension();

}
