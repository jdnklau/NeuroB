package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class PredicateDbMigration
        implements TrainingSetMigration {

    /** Format of the data to be migrated */
    private final PredicateDbFormat<PredDbEntry> sourceFormat;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDbMigration.class);

    /**
     * Instantiates a new data base migration, starting with the {@link JsonDbFormat}.
     */
    public PredicateDbMigration() {
        this(null);
    }

    /**
     * @param sourceFormat Format of the data to be migrated
     */
    public PredicateDbMigration(PredicateDbFormat<PredDbEntry> sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    @Override
    public DataGenerationStats migrate(Path source, Path targetDirectory, PredicateDbFormat targetFormat)
            throws IOException {
        DataGenerationStats stats = new DataGenerationStats();

        log.info("Migrating predicate data base from {} to {}", source, targetDirectory);

        Stream<Path> files = Files.walk(source);
        files.filter(Files::isRegularFile)
                .parallel()
                .filter(file -> file.toString().endsWith(sourceFormat.getFileExtension())) // only format files
                .forEach(dbFile -> {
                    try {
                        log.info("Migrating {}", dbFile);
                        stats.increaseFilesSeen();
                        DataGenerationStats fileStats =
                                migrateFile(dbFile, source, targetDirectory, targetFormat);
                        stats.mergeWith(fileStats);
                        log.info("Finished migration of {}", dbFile);
                    } catch (IOException e) {
                        log.warn("Unable to migrate {}", dbFile, e);
                        stats.increaseFilesWithErrors();
                    }
                });

        log.info("Migration finished: {}", stats);
        return stats;
    }

    /**
     * Migrates all the database files in the sourceDir into the targetDir,
     * translating the data according to the given feature and label generators.
     * <p>
     * The data is stored in the specified format.
     *
     * @param sourceDir
     * @param targetDir
     * @param generationSource Path to directory containing original B machines the data
     *         base was build upon.
     * @param featureGen
     * @param labelTrans
     * @param targetFormat
     * @param <D>
     * @param <L>
     *
     * @return
     *
     * @throws IOException
     */
    public <D extends Features, L extends Labelling>
    DataGenerationStats migrate(Path sourceDir, Path targetDir, Path generationSource,
            PredicateFeatureGenerating<D> featureGen,
            LabelTranslation<PredDbEntry, L> labelTrans,
            TrainingDataFormat<D, L> targetFormat) throws IOException {

        DataGenerationStats stats = new DataGenerationStats();

        log.info("Migrating predicate data base from {} to {}", sourceDir, targetDir);

        Stream<Path> files = Files.walk(sourceDir);
        files.filter(Files::isRegularFile)
                .parallel()
                .filter(file -> file.toString().endsWith(sourceFormat.getFileExtension())) // only format files
                .forEach(dbFile -> {
                    try {
                        log.info("Migrating {}", dbFile);
                        stats.increaseFilesSeen();
                        // Access original machine
                        BMachine origMachine = null;
                        if (generationSource != null) {
                            Path origPath = null;
                            try {
                                origPath = sourceFormat.getDataSource(dbFile);
                                log.debug("Determined original source machine: {}", origPath);
                            } catch (IOException e) {
                                log.warn("Unable to determine original source machine for {}",
                                        dbFile, e);
                            }
                            origMachine = (origPath != null)
                                    ? new BMachine(generationSource.resolve(origPath))
                                    : null;
                        }

                        DataGenerationStats fileStats =
                                migrateFile(dbFile, sourceDir, targetDir, origMachine,
                                        featureGen, labelTrans, targetFormat);
                        stats.mergeWith(fileStats);

                        log.info("Finished migration of {}", dbFile);
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
     * @param sourceFile db file containing data to be migrated.
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
            Path targetDirectory, PredicateDbFormat<PredDbEntry> targetFormat)
            throws IOException {

        try (Stream<TrainingSample<BPredicate, PredDbEntry>> samples =
                     sourceFormat.loadSamples(sourceFile)) {

            Path dataSource = sourceFormat.getDataSource(sourceFile);
            Path sampleSource = dataSource != null
                    ? dataSource
                    : stripCommonSourceDir(sourceFile, commonSourceDirectory);

            TrainingData data = new TrainingData<>(
                    sampleSource,
                    samples);
            return targetFormat.writeSamples(data, targetDirectory);
        }
    }

    public <D extends Features, L extends Labelling>
    DataGenerationStats migrateFile(Path sourceFile, Path commonSourceDirectory,
            Path targetDirectory,
            PredicateFeatureGenerating<D> featureGen, LabelTranslation<PredDbEntry, L> labelTrans,
            TrainingDataFormat<D, L> targetFormat) throws IOException {
        return migrateFile(sourceFile, commonSourceDirectory, targetDirectory, null,
                featureGen, labelTrans, targetFormat);
    }

    public <D extends Features, L extends Labelling>
    DataGenerationStats migrateFile(Path sourceFile, Path commonSourceDirectory,
            Path targetDirectory, @Nullable BMachine origMachine,
            PredicateFeatureGenerating<D> featureGen, LabelTranslation<PredDbEntry, L> labelTrans,
            TrainingDataFormat<D, L> targetFormat) throws IOException {

        DataGenerationStats stats = new DataGenerationStats();

        // Access machine
        MachineAccess access = null;
        try {
            access = origMachine != null ? origMachine.spawnMachineAccess() : null;
        } catch (MachineAccessException e) {
            log.warn("Unable to access machine {} for migration context", origMachine, e);
        }

        MachineAccess finalAccess = access;
        try (Stream<TrainingSample<BPredicate, PredDbEntry>> rawSamples =
                     sourceFormat.loadSamples(sourceFile)) {
            Stream<TrainingSample<D, L>> samples =
                    rawSamples.map(sample -> {
                        try {
                            log.trace("Migrating sample {}", sample);
                            return migrateSample(sample, featureGen, labelTrans, finalAccess);
                        } catch (FeatureCreationException e) {
                            log.warn("Could not migrate {}", sample.getData(), e);
                            stats.increaseSamplesFailed();
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .onClose(() -> {
                        if (finalAccess != null)
                           finalAccess.close();
                    });

            TrainingData<D, L> data = new TrainingData<>(
                    stripCommonSourceDir(sourceFile, commonSourceDirectory),
                    samples);

            stats.mergeWith(targetFormat.writeSamples(data, targetDirectory));
            samples.close();
        }
        return stats;
    }

    /**
     * Translates a given sample from the data base according to the given mappings
     * for {@link de.hhu.stups.neurob.core.features.FeatureGenerating features}
     * and {@link de.hhu.stups.neurob.training.migration.labelling.LabelTranslation labels}.
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
    TrainingSample<D, L> migrateSample(TrainingSample<BPredicate, PredDbEntry> sample,
            PredicateFeatureGenerating<D> featureGen,
            LabelTranslation<PredDbEntry, L> labelTrans)
            throws FeatureCreationException {
        return migrateSample(sample, featureGen, labelTrans, null);
    }

    /**
     * Translates a given sample from the data base according to the given mappings
     * for {@link de.hhu.stups.neurob.core.features.FeatureGenerating features}
     * and {@link de.hhu.stups.neurob.training.migration.labelling.LabelTranslation labels}.
     *
     * @param sample
     * @param featureGen
     * @param labelTrans
     * @param origMachine Access to original B machine the sample was generated from
     * @param <D>
     * @param <L>
     *
     * @return
     */
    <D extends Features, L extends Labelling>
    TrainingSample<D, L> migrateSample(TrainingSample<BPredicate, PredDbEntry> sample,
            PredicateFeatureGenerating<D> featureGen, LabelTranslation<PredDbEntry, L> labelTrans,
            MachineAccess origMachine)
            throws FeatureCreationException {

        BPredicate predicate = sample.getData();
        log.trace("Migrating {}", predicate);
        D features = featureGen.generate(predicate, origMachine);
        L labelling = labelTrans.translate(sample.getLabelling());
        log.trace("Migrated {} to {}/{}", predicate, features, labelling);
        return new TrainingSample<>(features, labelling, sample.getSourceFile());
    }

}
