package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Format for predicate list files.
 * <p>
 * A predicate list file is simply a text file in which each line has the form
 * {@code source:predicate} where {@code source} is the path to the originating source B machine.
 */
public class SimplePredicateList implements PredicateDbFormat<PredDbEntry> {

    public static String EXT = "preds.txt";

    private static final Logger log =
            LoggerFactory.getLogger(SimplePredicateList.class);

    @Override
    public Path getDataSource(Path dbFile) throws IOException {
        BufferedReader reader = Files.newBufferedReader(dbFile);
        return Paths.get(reader.readLine());
    }

    @Override
    public DataGenerationStats shuffleWithBuckets(Path source, int numBuckets, Path targetDir, Random rng) throws IOException {
        // Tbd.
        return null;
    }

    @Override
    public DataGenerationStats writeSamples(
            TrainingData<BPredicate, PredDbEntry> trainingData,
            Path targetDirectory) throws IOException {
        DataGenerationStats stats = new DataGenerationStats();
        Path sourceFile = trainingData.getSourceFile();
        Path targetFile = getTargetLocation(sourceFile, targetDirectory);

        // Ensure target subdirectory exists
        Path targetSubdir = targetFile.getParent();
        try {
            log.trace("Creating directory {}", targetSubdir);
            Files.createDirectories(targetSubdir);
        } catch (IOException e) {
            log.error("Could not create target directory {}",
                    targetSubdir, e);
            return stats;
        }

        log.info("Writing samples from {} to {}", sourceFile, targetFile);
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile)) {
            DataGenerationStats writeStats =
                    writeSamples(trainingData, writer);
            writer.close();

            stats.increaseFilesCreated();
            stats.mergeWith(writeStats);
        } catch (IOException e) {
            log.warn("Could not create samples", e);
        } finally {
            log.trace("Closed write access to {}", targetFile);
        }

        return stats;
    }

    public DataGenerationStats writeSamples(TrainingData<BPredicate, PredDbEntry> trainingData,
                                            Writer writer) throws IOException {
        DataGenerationStats stats = new DataGenerationStats();

        Path source = trainingData.getSourceFile();

        // Sort data and make them unique.
        Stream<String> samplePredicates = trainingData.getSamples()
                .map(TrainingSample::getData)
                .map(BPredicate::getPredicate)
                .sorted().distinct();

        String prefix = source.toString() + ":";

        samplePredicates.forEach(s -> {
            try {
                writer.write(prefix);
                writer.write(s);
                writer.write("\n");
                stats.increaseSamplesWritten();
            } catch (IOException e) {
                log.warn("Unable to write {} to predicate list for {}",
                        s, source);
            }
        });

        writer.flush();
        return stats;
    }

    public String getMachineHash(Path sourceFile) {
        if (sourceFile == null) {
            log.warn("Unable to generate hash for null file");
            return "Hash error: File was null";
        }

        try {
            InputStream inputStream = new FileInputStream(sourceFile.toFile());
            return DigestUtils.sha512Hex(inputStream);
        } catch (FileNotFoundException e) {
            log.warn("Unable to generate hash for {}", sourceFile, e);
            return "Hash error: File not found";
        } catch (IOException e) {
            log.warn("Unable to generate hash for {}", sourceFile, e);
            return "Hash error: File not accessible";
        }
    }

    @Override
    public Stream<TrainingSample<BPredicate, PredDbEntry>> loadSamples(Path sourceFile) throws IOException {
        BufferedReader reader = Files.newBufferedReader(sourceFile);

        return reader.lines()
                .map(s -> {
                    s = s.trim();
                    int dividerPos = s.indexOf(':');

                    String sourceString = s.substring(0, dividerPos);
                    BMachine source;
                    if (sourceString.isEmpty() || sourceString.equals("none")) {
                        source = BMachine.EMPTY;
                    } else {
                        source = new BMachine(sourceString);
                    }
                    BPredicate pred =  BPredicate.of(s.substring(dividerPos + 1));

                    return new TrainingSample<>(pred, new PredDbEntry(pred, source, new HashMap<>()));
                });
    }

    @Override
    public String getFileExtension() {
        return EXT;
    }
}
