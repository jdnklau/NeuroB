package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.backends.KodKodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.db.TrainingDbFormat;
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

public class PredicateDumpFormat extends PredicateDbFormat {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDumpFormat.class);

    @Override
    public Path getTargetLocation(Path sourceFile, Path targetDirectory) {
        // FIXME: Remove this once PredicateDbFormat is a valid interface
        // If it is an interface, make use of default implementation

        // Get String representation
        String source;
        if (sourceFile == null) {
            source = "null." + getFileExtension();
        } else {
            source = sourceFile.toString();
        }

        // Replace extension
        int extPos = source.lastIndexOf('.');
        String target = source.substring(0, extPos +1 ) + getFileExtension();

        return targetDirectory.resolve(target);
    }

    /**
     * Streams the translated samples from the given *.pdump file.
     *
     * @param sourceFile A predicate dump file
     *
     * @return Stream of DbSamples
     */
    @Override
    public Stream<DbSample<BPredicate>> loadSamples(Path sourceFile) throws IOException {
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
            int splitPos = entry.indexOf(":");
            String sourceEntry = entry.substring(splitPos + 1);
            srcReference.set(Paths.get(sourceEntry));

            return null;
        }

        return new PredicateDump(entry, srcReference.get());
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

    @Override
    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<PredicateFeatures, L> trainingData,
            Path targetDirectory) {
        Path targetFile = getTargetLocation(trainingData.getSourceFile(), targetDirectory);
        log.info("Writing samples from {} to {}", trainingData.getSourceFile(), targetFile);

        DataGenerationStats stats = new DataGenerationStats();
        try {
            // Ensure directory exists
            Files.createDirectories(targetFile.getParent());
            BufferedWriter out = Files.newBufferedWriter(targetFile);
            stats.increaseFilesCreated();
            writeSamples(trainingData, out);
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
    DataGenerationStats writeSamples(TrainingData<PredicateFeatures, L> trainingData, Writer out) {
        DataGenerationStats stats = new DataGenerationStats();

        // keep track of current source, so adding a new annotation is possible on demand
        AtomicReference<Path> currentSource = new AtomicReference<>();
        trainingData.getSamples()
                .forEach(sample -> {
                    // Setup entry
                    String entry = sample.getLabelling().getLabellingString()
                                   + ":" + sample.getFeatures().getPredicate();
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
}
