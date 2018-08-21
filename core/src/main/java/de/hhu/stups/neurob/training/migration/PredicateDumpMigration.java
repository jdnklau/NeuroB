package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.backends.KodKodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class PredicateDumpMigration {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDumpMigration.class);

    public DataGenerationStats migrate(Path source, Path targetDirectory, PredicateDbFormat format)
            throws IOException {
        DataGenerationStats stats = new DataGenerationStats();

        log.info("Migrating predicate dump from {} to {}", source, targetDirectory);

        Stream<Path> files = Files.walk(source);
        files.filter(Files::isRegularFile)
                .parallel()
                .filter(file -> file.toString().endsWith(".pdump")) // only *.pdump
                .forEach(pdump -> {
                    try {
                        TrainingData data = new TrainingData(
                                stripCommonSourceDir(pdump, source),
                                streamTranslatedSamples(pdump).map(this::translate));
                        stats.mergeWith(migrateFile(pdump, source, targetDirectory, format));
                    } catch (IOException e) {
                        log.warn("Unable to migrate {}", pdump, e);
                        stats.increaseFilesWithErrors();
                    }
                });

        log.info("Migration finished: {}", stats);
        return stats;
    }

    private Path stripCommonSourceDir(Path sourceFile, Path commonSourceDir) {
        if (commonSourceDir.equals(sourceFile)) {
            return sourceFile.getFileName();
        }
        return commonSourceDir.relativize(sourceFile);
    }

    /**
     * Migrate the data into the target location in the respective format
     *
     * @param sourceFile *.pdump file containing data to be migrated.
     * @param targetDirectory Directory into which the data will be migrated.
     *         Depending on the format, this might result in a single file or multiple.
     * @param format Target format the data shall be migrated into.
     *
     * @return Generation statistics
     *
     * @throws IOException
     */
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
     * @param format Target format the data shall be migrated into.
     *
     * @return Generation statistics
     *
     * @throws IOException
     */
    public DataGenerationStats migrateFile(Path sourceFile, Path commonSourceDirectory,
            Path targetDirectory, PredicateDbFormat format) throws IOException {

        TrainingData data = new TrainingData(
                stripCommonSourceDir(sourceFile, commonSourceDirectory),
                streamTranslatedSamples(sourceFile).map(this::translate));

        return format.writeSamples(data, targetDirectory);
    }

    /**
     * Translate a given Predicate Dump into a DbSample.
     *
     * @param pdump
     *
     * @return
     */
    public DbSample<BPredicate> translate(PredicateDump pdump) {
        BPredicate predicate = pdump.getPredicate();
        Labelling labels = new Labelling(
                pdump.getTime(ProBBackend.class),
                pdump.getTime(KodKodBackend.class),
                pdump.getTime(Z3Backend.class),
                pdump.getTime(SmtBackend.class)
        );
        Path source = pdump.getSource();

        return new DbSample<>(predicate, labels, source);
    }

    public TrainingSample<PredicateFeatures, Labelling> translate(DbSample<BPredicate> dbSample) {
        Path source = dbSample.getSourceMachine();
        PredicateFeatures features = new PredicateFeatures(dbSample.getBElement().getPredicate());
        Labelling labels = dbSample.getLabelling();

        return new TrainingSample<>(features, labels, source);
    }

    /**
     * Streams the translated samples from the given *.pdump file.
     *
     * @param source A predicate dump file
     *
     * @return Stream of DbSamples
     */
    public Stream<DbSample<BPredicate>> streamTranslatedSamples(Path source) throws IOException {
        Stream<String> entries = Files.lines(source);

        // #source annotations define the source machine for all following
        // lines; value is stored in sourceMch
        AtomicReference<Path> sourceMch = new AtomicReference<>();
        return entries
                // Handle source annotations
                .peek(line -> {
                    if (line.startsWith("#source:")) {
                        int splitPos = line.indexOf(":");
                        String sourceEntry = line.substring(splitPos + 1);
                        sourceMch.set(Paths.get(sourceEntry));
                    }
                })
                // skip comments/annotation lines
                .filter(line -> !line.startsWith("#"))
                .map(entry -> new PredicateDump(entry, sourceMch.get()))
                .map(this::translate);
    }

}
