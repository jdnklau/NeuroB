package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.features.FeatureGenerating;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TrainingDataGenerator<F extends Features, L extends Labelling> {

    protected final TrainingDataFormat format;
    protected final FeatureGenerating<F,?> featureGenerator;

    protected static final Logger log =
            LoggerFactory.getLogger(TrainingDataGenerator.class);

    public TrainingDataGenerator(
            FeatureGenerating<F,?>  featureGenerator,
            TrainingDataFormat format) {
        this.format = format;
        this.featureGenerator = featureGenerator;
    }

    /**
     * Creates the training data from respective source files in
     * <code>source</code> and writes them to <code>targetDir</code>.
     * <p>
     * In <code>targetDir</code>, a subdirectory is created,
     * named after the combination of the Labelling and Features in use.
     * Already existing training data is not generated anew, but kept.
     * </p>
     *
     * @param source path to single file or directory
     * @param targetDir path to target directory
     *
     * @see #generateTrainingData(Path, Path, boolean)
     */
    public void generateTrainingData(Path source, Path targetDir)
            throws IOException {
        generateTrainingData(source, targetDir, true);
    }

    /**
     * Creates the training data from respective source files in
     * <code>source</code> and writes them to <code>targetDir</code>.
     * <p>
     * In <code>targetDir</code>, a subdirectory is created,
     * named after the combination of the Labelling and Features in use.
     * </p>
     *
     * @param source path to single file or directory
     * @param targetDir path to target directory
     * @param lazy whether existing training data should be kept
     *         (<code>true</code>) or freshly generated.
     */
    public void generateTrainingData(Path source, Path targetDir,
            boolean lazy) throws IOException {

        // TODO: Set target dir to targetDir/labelling/features/
        final Path fullTargetDir = targetDir;
//                .resolve(labelling.getClass().getSimpleName())
//                .resolve(features.getClass().getSimpleName());
        log.info("Generating training data from {}, storing in {}",
                source, fullTargetDir);

        // TODO: add statistics?
        try (Stream<Path> sourceFiles = Files.list(source)) {
            sourceFiles
                    .parallel()
                    .filter(Files::isRegularFile)
                    // Only create if non-lazy or non-existent
                    .filter(file -> !lazy && !dataAlreadyExists(file,
                            format.getTargetLocation(file, fullTargetDir)))
                    .map(file -> streamSamplesFromFile(file))
                    .forEach(samples ->
                            format.writeSamples(samples, fullTargetDir));
        }

        log.info("Generation of training data: done");
    }

    /**
     * Creates a list of training samples from the given file.
     *
     * @param file
     */
    public List<TrainingSample<F, L>> generateSamplesFromFile(
            Path file) {
        return streamSamplesFromFile(file).collect(Collectors.toList());
    }

    /**
     * Creates a stream of training samples from the given file.
     *
     * @param file
     */
    public abstract Stream<TrainingSample<F, L>> streamSamplesFromFile(
            Path file);


    /**
     * Indicates whether all possible training samples to be generated otherwise
     * are already existent in the target location.
     *
     * @param sourceFile
     * @param targetLocation
     *
     * @return
     */
    public abstract boolean dataAlreadyExists(Path sourceFile,
            Path targetLocation);

}
