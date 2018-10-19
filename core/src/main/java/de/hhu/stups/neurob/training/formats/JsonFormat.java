package de.hhu.stups.neurob.training.formats;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonFormat implements TrainingDataFormat<Features, Labelling> {

    private static final Logger log = LoggerFactory.getLogger(JsonFormat.class);

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public DataGenerationStats writeSamples(TrainingData<Features, Labelling> trainingData, Path targetDirectory) {
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
            BufferedWriter out = Files.newBufferedWriter(targetFile);
            DataGenerationStats writeStats = writeSamples(trainingData.getSamples(),
                    out);
            out.close();

            stats.increaseFilesCreated();
            stats.mergeWith(writeStats);
        } catch (IOException e) {
            log.warn("Could not create samples", e);
        }

        return stats;
    }

    @Override
    public Stream<TrainingSample<Features, Labelling>> loadSamples(Path sourceFile) throws IOException {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new JsonIterator(Files.newBufferedReader(sourceFile)),
                        0),
                false);
    }

    public DataGenerationStats writeSamples(Stream<TrainingSample<Features, Labelling>> samples, Writer out) throws IOException {
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

    public static class JsonIterator
            implements Iterator<TrainingSample<Features, Labelling>> {

        private final JsonReader reader;
        private boolean hasEnded;

        public JsonIterator(Reader reader) throws IOException {
            this.reader = new JsonReader(reader);
            this.hasEnded = false;

            consumeHeader();
        }

        public void consumeHeader() throws IOException {
            // Should start as a JSON-Object containing the "samples" to Array
            // of objects mapping
            reader.beginObject();
            String samples = reader.nextName();
            if (!samples.equals("samples")) {
                throw new IOException(
                        "Json Format is invalid: Expected \"samples\" property,"
                        + "but got " + samples);
            }

            // "samples" maps to an array of objects
            reader.beginArray();
        }

        @Override
        public boolean hasNext() {
            if (hasEnded) {
                return false;
            }
            boolean hasNext = false;
            try {
                if (reader.peek().equals(JsonToken.BEGIN_OBJECT)) {
                    hasNext = true;
                } else {
                    verifyFooter();
                    hasEnded = true;
                }
            } catch (IOException e) {
                log.error("Could not peek next element from database", e);
                hasEnded = true;
            }
            return hasNext;
        }

        public void verifyFooter() throws IOException {
            reader.endArray();
            reader.endObject();
            if (!reader.peek().equals(JsonToken.END_DOCUMENT)) {
                log.warn("Json format not valid:"
                         + "found trailing elements that are ignored.");
            }
        }

        @Override
        public TrainingSample<Features, Labelling> next() {
            try {
                reader.beginObject();
                TrainingSample<Features, Labelling> sample = readNextSample();
                reader.endObject();

                return sample;
            } catch (IOException e) {
                log.error("Could not read Json any further", e);
                hasEnded = true;
                return null;
            }

        }

        private TrainingSample<Features, Labelling> readNextSample() throws IOException {
            Features features;
            Labelling labelling;

            // read first and second keyword
            String key1 = reader.nextName();
            Double[] key1Array = readNextDataArray();
            String key2 = reader.nextName();
            Double[] key2Array = readNextDataArray();

            // associate them with respective semantic
            if (key1.equals("features") && key2.equals("labelling")) {
                features = new Features(key1Array);
                labelling = new Labelling(key2Array);
            } else if (key2.equals("features") && key1.equals("labelling")) {
                features = new Features(key2Array);
                labelling = new Labelling(key1Array);
            } else {
                throw new IOException(
                        "Unexpected pair of keys: " + key1 + ", " + key2);
            }

            return new TrainingSample<>(features, labelling);
        }

        public Double[] readNextDataArray() throws IOException {
            reader.beginArray();
            List<Double> doubles = new ArrayList<>();
            while(!reader.peek().equals(JsonToken.END_ARRAY)) {
                doubles.add(reader.nextDouble());
            }
            reader.endArray();
            return doubles.toArray(new Double[0]);
        }
    }
}
