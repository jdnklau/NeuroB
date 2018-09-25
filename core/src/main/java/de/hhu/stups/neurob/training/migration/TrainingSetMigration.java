package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for migrating a training database to another, more suited data base format
 */
public interface TrainingSetMigration {

    /**
     * Migrates data from the source directory to the target directory,
     * hereby translating the data into the target format.
     *
     * @param source
     * @param targetDirectory
     * @param targetFormat
     *
     * @return
     *
     * @throws IOException
     */
    DataGenerationStats migrate(Path source, Path targetDirectory, PredicateDbFormat targetFormat)
            throws IOException;

    /**
     * Migrates the data into the target location in the respective format
     *
     * @param sourceFile DB file containing data to be migrated.
     * @param targetDirectory Directory into which the data will be migrated.
     *         Depending on the format, this might result in a single file or multiple.
     * @param format Target format the data shall be migrated into.
     *
     * @return Generation statistics
     *
     * @throws IOException
     */
    DataGenerationStats migrateFile(Path sourceFile, Path targetDirectory,
            PredicateDbFormat format) throws IOException;
}
