package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface TrainingDbFormat<D, L extends Labelling>
        extends TrainingDataFormat<D, L> {

    /**
     * Streams Training Data from the source.
     * <p>
     * As this is an IO access to the given source, the stream needs to be closed
     * after use.
     *
     * @param source
     *
     * @return
     *
     * @throws IOException
     */
    default Stream<TrainingData<D, L>> loadTrainingData(Path source) throws IOException {
        return Files.walk(source)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(getFileExtension())) // only account for matching files
                .map(this::loadTrainingDataFromFile)
                .filter(Objects::nonNull);
    }

    /**
     * Streams Training Data from the source file.
     * <p>
     * As this is an IO access to the given source, the stream needs to be closed
     * after use.
     *
     * @param dbFile
     *
     * @return
     */
    default TrainingData<D, L> loadTrainingDataFromFile(Path dbFile) {
        final Logger log = LoggerFactory.getLogger(TrainingDbFormat.class);
        try {
            // Check whether file is valid
            if (!this.isValidFile(dbFile)) {
                log.warn("Found invalid file {}", dbFile);
                return null;
            }
            return new TrainingData<>(
                    this.getDataSource(dbFile),
                    this.loadSamples(dbFile)); // FIXME: Is this getting properly closed?
        } catch (IOException e) {
            log.error("Unable to access {}", dbFile, e);
            return null;
        }
    }


    /**
     * Retrieves the original path of the B machine from which the dbFile was created.
     * <p>
     * The promise is, that if the dbFile at least references one file as original source,
     * the first such file is returned. Might be null if no such file exists.
     *
     * @param dbFile Path to data base file created by this format.
     *
     * @return First referenced source machine in dbFile or null
     */
    Path getDataSource(Path dbFile) throws IOException;


    /**
     * Splits data from the given source into the directories {@code first} and {@code second}.
     * <p>
     * Each sample from the source is placed into the first directory for a probability of
     * {@code ratioFirst}. Otherwise it is placed into the second directory.
     * The specified probability has to be between 0 and 1.
     *
     * @param source Source of the training data to split.
     * @param first Location of the samples by the specified ratio.
     * @param second Location of the samples by the complement ratio.
     * @param ratioFirst The ratio of samples partitioned into the first location over all
     *         samples
     *
     * @return Array with two entries of DataGenerationStats, index 0 corresponding to the first,
     *         index 1 corresponding to the second location.
     */
    default DataGenerationStats[] splitData(Path source, Path first, Path second, Double ratioFirst)
            throws IOException {
        int seed = new Random().nextInt();
        return splitData(source, first, second, ratioFirst, seed);
    }

    /**
     * Splits data from the given source into the directories {@code first} and {@code second}.
     * <p>
     * Each sample from the source is placed into the first directory for a probability of
     * {@code ratioFirst}. Otherwise it is placed into the second directory.
     * The specified probability has to be between 0 and 1.
     *
     * @param source Source of the training data to split.
     * @param first Location of the samples by the specified ratio.
     * @param second Location of the samples by the complement ratio.
     * @param ratioFirst The ratio of samples partitioned into the first location over all
     *         samples
     * @param seed Seed for the Random Number Generator
     *
     * @return Array with two entries of DataGenerationStats, index 0 corresponding to the first,
     *         index 1 corresponding to the second location.
     */
    default DataGenerationStats[] splitData(Path source, Path first, Path second,
            Double ratioFirst, int seed)
            throws IOException {
        final Logger log = LoggerFactory.getLogger(TrainingDbFormat.class);

        DataGenerationStats[] stats = {new DataGenerationStats(), new DataGenerationStats()};

        Random rng = new Random(seed);

        loadTrainingData(source).map(d -> d.split(ratioFirst, rng))
                .forEach(data -> {
                    try {
                        stats[0].mergeWith(writeSamples(data[0], first));
                        stats[1].mergeWith(writeSamples(data[1], second));
                    } catch (IOException e) {
                        log.error("Unable to split data from {}", data[0].getSourceFile(), e);
                    }
                });

        return stats;
    }

    default DataGenerationStats copyShuffled(Path source, Path target, Random rng) throws IOException {
        final Logger log = LoggerFactory.getLogger(TrainingDbFormat.class);

        List<TrainingSample<D, L>> samples = loadTrainingData(source)
                .flatMap(TrainingData::getSamples)
                .collect(Collectors.toList());

        Collections.shuffle(samples, rng);

        TrainingData<D, L> shuffled = new TrainingData<>(source, samples.stream());

        return writeSamples(shuffled, target);
    }

    /**
     * Shuffles the data set located at {@code source} by splitting it into
     * the specified amount of buckets first, shuffling those,
     * then assembling the data from all buckets into a single file file.
     * <p>
     * Let M be the number of buckets. Assuming N training samples in the source,
     * M files (bucket-0.ext .. bucket-(M-1).ext with .ext being the
     * {@link #getFileExtension() respective file extension})
     * will be created in the targetDir.
     * Each such bucket file will contain N/M samples on average
     * (the distribution will happen uniformly but random).
     * Chose an M which is small enough so it is possible to create M files and hold
     * M open file handles,
     * yet large enough that N/M samples fit easily into the available memory.
     * <p>
     * The resulting shuffled data file will be located at targetDir/shuffled.ext.
     *
     * @param source Path to the database
     * @param numBuckets Number of buckets (M) to be used for shuffling
     * @param targetDir Target directory to contain the shuffled data
     * @param rng Random number generator which decides the splitting into buckets and
     *         following shuffling
     *
     * @return
     *
     * @throws IOException
     */
    DataGenerationStats shuffleWithBuckets(Path source, int numBuckets, Path targetDir, Random rng)
            throws IOException;
}
