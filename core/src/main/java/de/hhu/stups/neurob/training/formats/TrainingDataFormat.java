package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Storage format for training data.
 * <p>
 * Stores labelled training data on disk.
 *
 * @param <D> Type of stored data
 * @param <L> Type of corresponding labels
 */
public interface TrainingDataFormat<D, L extends Labelling> {

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
        String target = source.substring(0, extPos + 1) + getFileExtension();

        return targetDirectory.resolve(target);
    }

    /**
     * Writes the given <code>trainingData</code> into the target location
     * in the respective format.
     *
     * @param trainingData
     * @param targetDirectory Location to populate with training data
     */
    DataGenerationStats writeSamples(TrainingData<D, L> trainingData,
            Path targetDirectory) throws IOException;

    /**
     * Streams training samples from the given source file.
     * <p>
     * As this is an IO access to the given sourceFile, the stream
     * needs to be closed after use.
     *
     * @param sourceFile Path to a file conforming this format.
     *
     * @return Stream of training samples.
     *
     * @throws IOException
     */
    Stream<TrainingSample<D, L>> loadSamples(Path sourceFile) throws IOException;

    /**
     * Returns the extension used for written files.
     * <p>
     * If the extension for example would be "ext", generated files will
     * take names like "generated_file.ext".
     * <p>
     * The returned extension is without the leading dot.
     *
     * @return
     */
    String getFileExtension();

    /**
     * Checks whether the file in the given path matches this format.
     *
     * @param file
     *
     * @return
     */
    default Boolean isValidFile(Path file) {
        return file.toString().endsWith(getFileExtension());
    }

}
