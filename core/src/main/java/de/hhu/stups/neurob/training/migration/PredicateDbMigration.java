package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PredicateDbMigration implements TrainingSetMigration {

    /** Format of the data to be migrated */
    private final PredicateDbFormat sourceFormat;

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDbMigration.class);

    public PredicateDbMigration() {
        this(new JsonDbFormat());
    }

    /**
     * @param sourceFormat Format of the data to be migrated
     */
    public PredicateDbMigration(PredicateDbFormat sourceFormat) {
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
            Path targetDirectory, PredicateDbFormat targetFormat) throws IOException {

        TrainingData data = new TrainingData(
                stripCommonSourceDir(sourceFile, commonSourceDirectory),
                sourceFormat.loadSamples(sourceFile));

        return targetFormat.writeSamples(data, targetDirectory);
    }

}
