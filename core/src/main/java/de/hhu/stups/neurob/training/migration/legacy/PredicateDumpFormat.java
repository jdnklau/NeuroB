package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class PredicateDumpFormat implements PredicateDbFormat<DecisionTimings> {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDumpFormat.class);

    /**
     * Streams the translated samples from the given *.pdump file.
     *
     * @param sourceFile A predicate dump file
     *
     * @return Stream of DbSamples
     */
    @Override
    public Stream<TrainingSample<BPredicate, DecisionTimings>> loadSamples(Path sourceFile) throws IOException {
        Stream<String> entries = Files.lines(sourceFile);

        // #source annotations define the source machine for all following
        // lines; value is stored in sourceMch
        AtomicReference<Path> sourceMch = new AtomicReference<>();
        return entries
                // skip comments/annotation lines that are not source annotations
                .filter(line -> !line.startsWith("#") || line.startsWith("#source:"))
                .map(line -> translateEntry(line, sourceMch)) // FIXME ugly state manipulation in stream
                .filter(Objects::nonNull) // due to stateful mapping, null is needed; filter it out
                .map(this::translate);
    }

    /**
     * Translates a predicate dump entry line into a {@link PredicateDump}
     * data structure. If the entry happens to be a #source annotation,
     * it instead changes the srcReference to the new source and returns null.
     *
     * @param entry
     * @param srcReference
     *
     * @return A PredicateDump instance or null
     */
    private PredicateDump translateEntry(String entry, AtomicReference<Path> srcReference) {
        if (entry.startsWith("#source:")) {
            String sourceEntry = entry.substring(8);
            srcReference.set(Paths.get(sourceEntry));

            return null;
        }

        return new PredicateDump(entry, srcReference.get());
    }

    /**
     * Translate a given Predicate Dump into a {@link TrainingSample}.
     *
     * @param pdump
     *
     * @return
     */
    public TrainingSample<BPredicate, DecisionTimings> translate(PredicateDump pdump) {
        BPredicate predicate = pdump.getPredicate();
        DecisionTimings labels = new DecisionTimings(
                predicate, pdump.getTimings(), PredicateDump.BACKENDS_USED);
        Path source = pdump.getSource();

        return new TrainingSample<>(predicate, labels, source);
    }

    @Override
    public DataGenerationStats writeSamples(TrainingData<BPredicate, DecisionTimings> trainingData,
            Path targetDirectory) {
        Path targetFile = getTargetLocation(trainingData.getSourceFile(), targetDirectory);
        log.info("Writing samples from {} to {}", trainingData.getSourceFile(), targetFile);

        DataGenerationStats stats = new DataGenerationStats();
        stats.increaseFilesSeen();

        try {
            // Ensure directory exists
            Files.createDirectories(targetFile.getParent());
            BufferedWriter out = Files.newBufferedWriter(targetFile);
            stats.increaseFilesCreated();
            DataGenerationStats writeStats = writeSamples(trainingData, out);
            stats.mergeWith(writeStats);
        } catch (IOException e) {
            log.error("Could not write predicate dump to {}", targetFile);
            stats.increaseFilesWithErrors();
        }
        return stats;
    }

    /**
     * Appends the given training data to the given writer.
     *
     * @param trainingData
     * @param out Writer to append the output to
     * @param <L>
     *
     * @return
     *
     * @throws IOException
     */
    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<BPredicate, L> trainingData, Writer out) {
        DataGenerationStats stats = new DataGenerationStats();

        // keep track of current source, so adding a new annotation is possible on demand
        AtomicReference<Path> currentSource = new AtomicReference<>();
        trainingData.getSamples()
                .forEach(sample -> {
                    // Setup entry
                    String entry = sample.getLabelling().getLabellingString()
                                   + ":" + sample.getData().getPredicate();
                    try {
                        addNewSourceIfNecessary(sample.getSourceFile(), currentSource.get(), out);
                        // Update source
                        log.debug("Writing entry {}", entry);
                        currentSource.set(sample.getSourceFile());
                        out.append(entry).write("\n");
                        out.flush();
                        stats.increaseSamplesWritten();
                    } catch (IOException e) {
                        log.warn("Could not write entry {}", entry, e);
                        stats.increaseSamplesFailed();
                    }
                });

        return stats;
    }

    public void addNewSourceIfNecessary(Path newSource, Path oldSource, Writer out)
            throws IOException {
        if (newSource != null && newSource.equals(oldSource)) {
            // same as old source; do nothing
            return;
        } else if (newSource == null && oldSource == null) {
            // same as old source; do nothing
            return;
        } else {
            // new source is not null and is different from old sourde
            log.debug("New source detected, writing annotation for {}, old was {}",
                    newSource, oldSource);
            out.append("#source:")
                    .append(newSource.toString())
                    .append("\n");
        }
    }

    @Override
    public String getFileExtension() {
        return "pdump";
    }

    @Override
    public Path getDataSource(Path dbFile) throws IOException {
        return Files.lines(dbFile)
                .filter(line -> line.startsWith("#source:"))
                .map(line -> line.substring(8))
                .map(Paths::get)
                .findFirst().get();
    }
}
