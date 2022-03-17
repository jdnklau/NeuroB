package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Format for predicate list files.
 * <p>
 * A predicate list file is simply a text file in which each line corresponds to
 * a predicate.
 * Lines starting with :: indicate the path to the source file from which the predicates below were
 * created.
 */
public class PredicateList implements PredicateDbFormat<PredDbEntry> {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateList.class);

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
        writer.write(":: source ");
        writer.write(source.toString());
        writer.write("\n");
        writer.write(":: sha512 ");
        writer.write(getMachineHash(trainingData.getAbsoluteSourcePath()));
        writer.write("\n");

        // Sort data and make them unique.
        Stream<String> samplePredicates = trainingData.getSamples()
                .map(TrainingSample::getData)
                .map(BPredicate::getPredicate)
                .sorted().distinct();

        samplePredicates.forEach(s -> {
            try {
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

        final Path[] source = {null};
        return reader.lines()
                .map(s -> {
                    s = s.trim();
                    if (s.startsWith(":: source")) {
                        int fileNameStart = s.indexOf(' ', 3);
                        String fileName = s.substring(fileNameStart + 1);
                        source[0] = Paths.get(fileName);
                    } else if (!s.startsWith("::")) {
                        BPredicate p = BPredicate.of(s);
                        return new TrainingSample<>(
                                p,
                                new PredDbEntry(
                                        p, new BMachine(source[0]),
                                        new HashMap<>()));
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public String getFileExtension() {
        return "predlist.txt";
    }
}
