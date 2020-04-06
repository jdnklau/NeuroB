package de.hhu.stups.neurob.training.generation;

import de.hhu.stups.neurob.core.features.FeatureGenerating;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.LabelGenerating;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TrainingSetGenerator {

    protected final TrainingDataFormat format;
    protected final FeatureGenerating featureGenerator;
    protected final LabelGenerating labelGenerator;

    protected static final Logger log =
            LoggerFactory.getLogger(TrainingSetGenerator.class);

    public <F, L extends Labelling, D>
    TrainingSetGenerator(
            FeatureGenerating<F, D> featureGenerator,
            LabelGenerating<L, D> labelGenerator,
            TrainingDataFormat<? super F, ? super L> format) {
        this.format = format;
        this.featureGenerator = featureGenerator;
        this.labelGenerator = labelGenerator;
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
    public DataGenerationStats generateTrainingData(Path source, Path targetDir)
            throws IOException {
        return generateTrainingData(source, targetDir, true);
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
    public <F extends Features, L extends Labelling>
    DataGenerationStats generateTrainingData(Path source, Path targetDir,
            boolean lazy) throws IOException {
        return generateTrainingData(source, targetDir, lazy, new HashSet<>());
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
     * @param excluded Collection of excluded paths (either files or directories) relative to source
     */
    public <F extends Features, L extends Labelling>
    DataGenerationStats generateTrainingData(
            Path source, Path targetDir, boolean lazy, Collection<Path> excluded)
            throws IOException {

        final Path fullTargetDir = targetDir;
        log.info("Generating training data from {}, storing in {}",
                source, fullTargetDir);

        DataGenerationStats stats = new DataGenerationStats();
        try (Stream<Path> sourceFiles = Files.walk(source, FileVisitOption.FOLLOW_LINKS)) {
            sourceFiles.collect(Collectors.toList())
                    .parallelStream()
                    .filter(file -> file.toString().endsWith(".mch")
                                    || file.toString().endsWith(".bcm"))
                    // Skip excluded files
                    .filter(path -> !excluded.stream().anyMatch(
                                    ex -> stripCommonSourceDir(path, source).startsWith(ex)))
                    // Only create if non-lazy or non-existent
                    .filter(file -> {
                        boolean nonexistent = !lazy || !dataAlreadyExists(file,
                                format.getTargetLocation(
                                        stripCommonSourceDir(file, source),
                                        fullTargetDir));
                        if (!nonexistent) {
                            log.info("Skipping {}: Data already present", file);
                        }
                        return nonexistent;
                    })
                    .map(file -> new TrainingData(
                            stripCommonSourceDir(file, source),
                            file,
                            streamSamplesFromFile(file)))
                    .forEach(samples ->
                    {
                        try {
                            stats.increaseFilesSeen();
                            DataGenerationStats writeStats = format.writeSamples(samples, fullTargetDir);
                            samples.getSamples().close();
                            stats.mergeWith(writeStats);
                        } catch (IOException e) {
                            log.warn("Could not write all samples for {}",
                                    samples.getSourceFile());
                            stats.increaseFilesWithErrors();
                        }
                    });
        }

        log.info("Generation of training data: done");
        log.info("Generation statistics: {}", stats);
        return stats;
    }

    private Path stripCommonSourceDir(Path sourceFile, Path commonSourceDir) {
        if (commonSourceDir.equals(sourceFile)) {
            return sourceFile.getFileName();
        }
        return commonSourceDir.relativize(sourceFile);
    }

    /**
     * Creates a list of training samples from the given file.
     *
     * @param file
     */
    public List<TrainingSample> generateSamplesFromFile(
            Path file) {
        return streamSamplesFromFile(file).collect(Collectors.toList());
    }

    /**
     * Creates a stream of training samples from the given file.
     * The stream should be closed after use.
     *
     * @param file
     */
    public abstract Stream<TrainingSample> streamSamplesFromFile(
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
