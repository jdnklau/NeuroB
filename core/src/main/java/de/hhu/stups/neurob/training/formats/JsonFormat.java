package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class JsonFormat implements TrainingDataFormat<Features> {

    private static final Logger log = LoggerFactory.getLogger(JsonFormat.class);

    @Override
    public Path getTargetLocation(Path sourceFile, Path targetDirectory) {
        if (sourceFile == null) {
            return targetDirectory.resolve("null.json");
        }

        // change extension with .json
        String source = sourceFile.toString();
        int extPos = source.lastIndexOf('.');
        String sourceJson = source.substring(0, extPos) + ".json";

        return targetDirectory.resolve(sourceJson);
    }

    @Override
    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<Features, L> trainingData, Path targetDirectory) {
        Path sourceFile = trainingData.getSourceFile();
        Path targetFile = getTargetLocation(sourceFile,
                targetDirectory);

        DataGenerationStats stats = new DataGenerationStats();
        stats.increaseFilesSeen();

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

        try {
            log.info("Writing samples from {} to {}",
                    sourceFile, targetFile);
            DataGenerationStats writeStats = writeSamples(trainingData.getSamples(),
                    Files.newBufferedWriter(targetFile));

            stats.increaseFilesCreated();
            stats.mergeWith(writeStats);
        } catch (IOException e) {
            log.warn("Could not create samples", e);
        }

        return stats;
    }

    public <L extends Labelling>
    DataGenerationStats writeSamples(Stream<TrainingSample<Features, L>> samples, Writer out) throws IOException {
        DataGenerationStats stats = new DataGenerationStats();

        out.append("{\"samples\":[");

        samples.map(this::createJsonEntry)
                // Interleave with comma, skip first comma
                .flatMap(json -> Stream.of(",", json))
                .skip(1)
                .forEach(string -> {
                    try {
                        out.append(string);
                        if (!",".equals(string)) {
                            stats.increaseSamplesWritten();
                        }
                    } catch (IOException e) {
                        log.warn("Could not write JSON data: {}", string);
                        stats.increaseSamplesFailed();
                    }
                });

        out.append("]}");
        out.flush();

        return stats;
    }

    /**
     * Return a JSON Object as String, containing the source file,
     * the features, and the labels as fields.
     *
     * @param sample
     * @param <L>
     *
     * @return
     */
    public <L extends Labelling>
    String createJsonEntry(TrainingSample<Features, L> sample) {
        Features features = sample.getFeatures();
        Labelling labels = sample.getLabelling();

        StringBuilder jsonStr = new StringBuilder();
        jsonStr.append("{");
        // only print source file if not null
        if (sample.getSourceFile() != null) {
            jsonStr.append("\"sourceFile\":")
                    .append("\"").append(sample.getSourceFile()).append("\",");
        }
        jsonStr.append("\"features\":")
                .append("[").append(features.getFeatureString()).append("],")
                .append("\"labelling\":")
                .append("[").append(labels.getLabellingString()).append("]")
                .append("}");

        return jsonStr.toString();
    }
}
