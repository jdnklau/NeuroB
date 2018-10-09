package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.features.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class PredicateDbMigration
        implements TrainingSetMigration {

    /** Format of the data to be migrated */
    private final PredicateDbFormat<DecisionTimings> sourceFormat;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDbMigration.class);

    /**
     * Instantiates a new data base migration, starting with the {@link JsonDbFormat}.
     */
    public PredicateDbMigration() {
        this(new JsonDbFormat());
    }

    /**
     * @param sourceFormat Format of the data to be migrated
     */
    public PredicateDbMigration(PredicateDbFormat<DecisionTimings> sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    @Override
    public DataGenerationStats migrate(
            Path source, Path targetDirectory, PredicateDbFormat targetFormat)
            throws IOException {
        DataGenerationStats stats = new DataGenerationStats();

        log.info("Migrating predicate data base from {} to {}", source, targetDirectory);

        Stream<Path> files = Files.walk(source);
        files.filter(Files::isRegularFile)
                .parallel()
                .filter(file -> file.toString().endsWith(sourceFormat.getFileExtension())) // only format files
                .forEach(dbFile -> {
                    try {
                        DataGenerationStats fileStats =
                                migrateFile(dbFile, source, targetDirectory, targetFormat);
                        stats.mergeWith(fileStats);
                    } catch (IOException e) {
                        log.warn("Unable to migrate {}", dbFile, e);
                        stats.increaseFilesWithErrors();
                    }
                });

        log.info("Migration finished: {}", stats);
        return stats;
    }

    public <D extends Features, L extends Labelling>
    DataGenerationStats migrate(Path sourceDir, Path targetDir,
            PredicateFeatureGenerating<D> featureGen,
            LabelTranslation<DecisionTimings, L> labelTrans,
            TrainingDataFormat<D, L> targetFormat) throws IOException {

        DataGenerationStats stats = new DataGenerationStats();

        log.info("Migrating predicate data base from {} to {}", sourceDir, targetDir);

        Stream<Path> files = Files.walk(sourceDir);
        files.filter(Files::isRegularFile)
                .parallel()
                .filter(file -> file.toString().endsWith(sourceFormat.getFileExtension())) // only format files
                .forEach(dbFile -> {
                    try {
                        DataGenerationStats fileStats =
                                migrateFile(dbFile, sourceDir, targetDir,
                                        featureGen, labelTrans, targetFormat);
                        stats.mergeWith(fileStats);
                    } catch (IOException e) {
                        log.warn("Unable to migrate {}", dbFile, e);
                        stats.increaseFilesWithErrors();
                    }
                });

        log.info("Migration finished: {}", stats);
        return stats;
    }

    private Path stripCommonSourceDir(Path sourceFile, Path commonSourceDir) {
        // TODO: Is this method still necessary? Directory is stripped in format now, isn't it?
        if (commonSourceDir.equals(sourceFile)) {
            return sourceFile.getFileName();
        }
        return commonSourceDir.relativize(sourceFile);
    }

    @Override
    public DataGenerationStats migrateFile(Path sourceFile, Path targetDirectory,
            PredicateDbFormat format) throws IOException {
        return migrateFile(sourceFile, sourceFile.getParent(), targetDirectory, format);
    }

    /**
     * Migrate the data into the target location in the respective format.
     *
     * @param sourceFile *.pdump file containing data to be migrated.
     * @param commonSourceDirectory Leading path of the source file to be discarded
     *         when calculating the name of the target location from the source.
     * @param targetDirectory Directory into which the data will be migrated.
     *         Depending on the format, this might result in a single file or multiple.
     * @param targetFormat Target format the data shall be migrated into.
     *
     * @return Generation statistics
     *
     * @throws IOException
     */
    public DataGenerationStats migrateFile(Path sourceFile, Path commonSourceDirectory,
            Path targetDirectory, PredicateDbFormat<? extends PredicateLabelling> targetFormat)
            throws IOException {

        TrainingData data = new TrainingData<>(
                stripCommonSourceDir(sourceFile, commonSourceDirectory),
                sourceFormat.loadSamples(sourceFile));

        return targetFormat.writeSamples(data, targetDirectory);
    }

    public <D extends Features, L extends Labelling>
    DataGenerationStats migrateFile(Path sourceFile, Path commonSourceDirectory,
            Path targetDirectory,
            PredicateFeatureGenerating<D> featureGen, LabelTranslation<DecisionTimings, L> labelTrans,
            TrainingDataFormat<D, L> targetFormat) throws IOException {

        DataGenerationStats stats = new DataGenerationStats();
        Stream<TrainingSample<D, L>> samples =
                sourceFormat.loadSamples(sourceFile)
                        .map(sample -> {
                            try {
                                return migrateSample(sample, featureGen, labelTrans);
                            } catch (FeatureCreationException e) {
                                log.warn("Could not migrate {}", sample.getData(), e);
                                stats.increaseSamplesFailed();
                                return null;
                            }
                        })
                        .filter(Objects::nonNull);

        TrainingData<D, L> data = new TrainingData<>(
                stripCommonSourceDir(sourceFile, commonSourceDirectory),
                samples);

        stats.mergeWith(targetFormat.writeSamples(data, targetDirectory));
        return stats;
    }

    /**
     * Translates a given sample from the data base according to the given mappings
     * for {@link de.hhu.stups.neurob.core.features.FeatureGenerating features}
     * and {@link com.sun.java.accessibility.util.java.awt.LabelTranslator labels}.
     *
     * @param sample
     * @param featureGen
     * @param labelTrans
     * @param <D>
     * @param <L>
     *
     * @return
     */
    <D extends Features, L extends Labelling>
    TrainingSample<D, L> migrateSample(TrainingSample<BPredicate, DecisionTimings> sample,
            PredicateFeatureGenerating<D> featureGen, LabelTranslation<DecisionTimings, L> labelTrans)
            throws FeatureCreationException {
        // TODO: Load state space again?
        BPredicate predicate = sample.getData();
        log.trace("Migrating {}", predicate);
        D features = featureGen.generate(predicate);
        L labelling = labelTrans.translate(sample.getLabelling());
        log.trace("Migrated {} to {}/{}", predicate, features, labelling);
        return new TrainingSample<>(features, labelling, sample.getSourceFile());
    }

}
