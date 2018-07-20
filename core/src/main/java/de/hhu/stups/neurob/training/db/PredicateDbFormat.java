package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.PredicateFeatures;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.JsonFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PredicateDbFormat implements TrainingDbFormat<PredicateFeatures, BPredicate> {

    private final TrainingDataFormat<? super PredicateFeatures> internalFormat;
    private final String[] backendsUsed = {
            "KodkodBackend",
            "ProBBackend",
            "SmtBackend",
            "Z3Backend",
    };

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDbFormat.class);

    public PredicateDbFormat() {
        internalFormat = new JsonFormat();
    }

    @Override
    public Stream<DbSample<BPredicate>> loadSamples(Path sourceFile)
            throws IOException {

        JsonReader reader = new JsonReader(Files.newBufferedReader(sourceFile));

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new PredicateDbIterator(reader),
                        0),
                false);
    }


    @Override
    public Path getTargetLocation(Path sourceFile, Path targetDirectory) {
        return internalFormat.getTargetLocation(sourceFile, targetDirectory);
    }

    @Override
    public <L extends Labelling>
    void writeSamples(TrainingData<PredicateFeatures, L> trainingData, Path targetDirectory)
            throws IOException {
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
            return;
        }

        try {
            log.info("Writing samples from {} to {}",
                    sourceFile, targetFile);
            writeSamples(trainingData,
                    Files.newBufferedWriter(targetFile));
        } catch (IOException e) {
            log.warn("Could not create samples", e);
        }
    }

    public <L extends Labelling>
    void writeSamples(TrainingData<PredicateFeatures, L> trainingData,
            Writer writer) throws IOException {
        // Header
        writer.write("{\"samples\"=[");
        trainingData.getSamples()
                .map(this::translateSampleToJsonObject)
                // Interleave with separating comma
                .flatMap(sample -> Stream.of(",", sample))
                .skip(1) // skip leading comma
                .forEach(sample -> {
                    try {
                        writer.write(sample);
                    } catch (IOException e) {
                        if (",".equals(sample)) {
                            log.error(
                                    "Unable to write separator between samples. "
                                    + "Resulting Data base file might be faulty",
                                    e);
                            throw new IllegalStateException(
                                    "Could not add separator between samples");
                        }
                        log.warn("Unable to write the sample {}", sample, e);
                    }
                });
        // Footer
        writer.write("]}");
        writer.flush();
    }

    public <L extends Labelling>
    String translateSampleToJsonObject(TrainingSample<PredicateFeatures, L> sample) {
        // Extract meta data from the sample
        String predicate = sample.getFeatures().getPredicate();
        boolean isSourceDefined = sample.getSourceFile() != null;
        String source = (isSourceDefined) ? sample.getSourceFile().toString() : "";

        // Extract Backend Data
        Map<String, String> backendTimes = new HashMap<>();
        Double[] labelTimes = sample.getLabelling().getLabellingArray();
        // Safety check: number of entries must match
        if (labelTimes.length != backendsUsed.length) {
            throw new IllegalArgumentException(
                    "Provided TrainingSample does not allow a 1:1 mapping of "
                    + "its contained labelled times to the expected backends "
                    + "in use.");
        }
        String timings = IntStream.range(0, backendsUsed.length)
                .mapToObj(index ->
                        "\"" + backendsUsed[index] + "\":" + labelTimes[index])
                .collect(Collectors.joining(","));

        // Concatenate everything into one Json Object
        String sourceEntry = (isSourceDefined)
                ? "\"source\":\"" + source + "\","
                : "";
        String json = "{"
                      + "\"predicate\":\"" + predicate + "\","
                      + sourceEntry
                      + "\"timings\":{" + timings + "}"
                      + "}";

        return json;
    }

    public static class PredicateDbIterator implements Iterator<DbSample<BPredicate>> {
        private final JsonReader reader;
        private boolean hasEnded;

        public PredicateDbIterator(JsonReader reader) throws IOException {
            this.reader = reader;
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

        public void verifyFooter() throws IOException {
            reader.endArray();
            reader.endObject();
            if (!reader.peek().equals(JsonToken.END_DOCUMENT)) {
                log.warn("Json format not valid:"
                         + "found trailing elements that are ignored.");
            }
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


        @Override
        public DbSample<BPredicate> next() {
            try {
                Map<String, String> sampleData = nextSampleData();

                return translateToDbSample(sampleData);
            } catch (IOException e) {
                log.error(
                        "Could not read next sample from data base properly", e);
                hasEnded = true;
            }
            return null;
        }

        public DbSample<BPredicate> translateToDbSample(Map<String, String> sampleData) {
            BPredicate pred = new BPredicate(sampleData.get("predicate"));
            Path source = null;
            if (sampleData.containsKey("source")) {
                source = Paths.get(sampleData.get("source"));
            }

            Labelling l = translateTimings(sampleData.get("timings"));

            return new DbSample<>(pred, l, source);
        }

        public Labelling translateTimings(String timings) {
            String[] solverTimes = timings.split(",");
            Double[] labels = new Double[solverTimes.length];
            for (int i = 0; i < solverTimes.length; i++) {
                String[] timeSplit = solverTimes[i].split(":");
                Double time = Double.parseDouble(timeSplit[1]);
                labels[i] = time;
            }
            return new Labelling(labels);
        }


        public Map<String, String> nextSampleData() throws IOException {
            Map<String, String> data = new HashMap<>();

            reader.beginObject();
            readSampleProperty(reader.nextName(), data);
            readSampleProperty(reader.nextName(), data);

            // Source is optional
            if (data.containsKey("source") || JsonToken.NAME.equals(reader.peek())) {
                readSampleProperty(reader.nextName(), data);
            }

            reader.endObject();

            return data;
        }

        public void readSampleProperty(String property,
                Map<String, String> data) throws IOException {
            if ("predicate".equals(property) || "source".equals(property)) {
                data.put(property, reader.nextString());
            } else if ("timings".equals(property)) {
                // Read entries
                reader.beginObject();
                List<String> solvers = new ArrayList<>();
                while (JsonToken.NAME.equals(reader.peek())) {
                    String solver = reader.nextName();
                    Double time = reader.nextDouble();
                    solvers.add(solver + ":" + time);
                }
                reader.endObject();
                solvers.sort(Comparator.naturalOrder());

                String solverString = String.join(",", solvers);
                data.put("timings", solverString);
            } else {
                log.warn("Unknown property \"{}\"", property);
                reader.skipValue();
            }
        }
    }
}
