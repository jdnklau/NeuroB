package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
     * Translates a sample pulled from the migrating data base into a
     * generic training sample.
     *
     * @param dbSample
     *
     * @return
     */
    TrainingSample<PredicateFeatures, Labelling> translate(DbSample<BPredicate> dbSample);

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
