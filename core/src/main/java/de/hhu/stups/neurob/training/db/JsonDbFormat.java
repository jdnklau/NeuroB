package de.hhu.stups.neurob.training.db;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonDbFormat implements PredicateDbFormat<DecisionTimings> {

    /**
     * Array over backends in use, also providing an ordering by id.
     * 0 - ProB
     * 1 - Kodkod
     * 2 - Z3
     * 3 - SMT_SUPPORTED_INTERPRETER
     */
    public final static Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

    /**
     * Label generator to get data for predicates.
     */
    public final static PredicateLabelGenerating<DecisionTimings> LABEL_GENERATOR =
            new DecisionTimings.Generator(3, BACKENDS_USED);


    private static final Logger log =
            LoggerFactory.getLogger(JsonDbFormat.class);

    @Override
    public Stream<TrainingSample<BPredicate, DecisionTimings>> loadSamples(Path sourceFile)
            throws IOException {
        JsonReader reader = new JsonReader(Files.newBufferedReader(sourceFile));

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new PredicateDbIterator(reader),
                        0),
                false);
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public DataGenerationStats writeSamples(TrainingData<BPredicate, DecisionTimings> trainingData, Path targetDirectory) {
        Path sourceFile = trainingData.getSourceFile();
        Path targetFile = getTargetLocation(sourceFile, targetDirectory);

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
            DataGenerationStats writeStats =
                    writeSamples(trainingData, Files.newBufferedWriter(targetFile));

            stats.increaseFilesCreated();
            stats.mergeWith(writeStats);
        } catch (IOException e) {
            log.warn("Could not create samples", e);
        }

        return stats;
    }

    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<BPredicate, L> trainingData,
            Writer writer) throws IOException {
        // Set up stats
        DataGenerationStats stats = new DataGenerationStats();

        // Header
        writer.write("{\"samples\":[");
        trainingData.getSamples()
                .map(this::translateSampleToJsonObject)
                // Interleave with separating comma
                .flatMap(sample -> Stream.of(",", sample))
                .skip(1) // skip leading comma
                .forEach(sample -> {
                    try {
                        writer.write(sample);
                        if (!",".equals(sample)) {
                            stats.increaseSamplesWritten();
                        }
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
                        stats.increaseSamplesFailed();
                    }
                });
        // Footer
        writer.write("]}");
        writer.flush();

        return stats;
    }

    public <L extends Labelling>
    String translateSampleToJsonObject(TrainingSample<BPredicate, L> sample) {
        // Extract meta data from the sample
        BPredicate predicate = sample.getData();
        boolean isSourceDefined = sample.getSourceFile() != null;
        String source = (isSourceDefined) ? sample.getSourceFile().toString() : "";

        // Extract Backend Data
        Map<String, String> backendTimes = new HashMap<>();
        Double[] labelTimes = sample.getLabelling().getLabellingArray();
        // Safety check: number of entries must match
        if (labelTimes.length != BACKENDS_USED.length) {
            throw new IllegalArgumentException(
                    "Provided TrainingSample does not allow a 1:1 mapping of "
                    + "its contained labelled times to the expected backends "
                    + "in use.");
        }
        String timings = IntStream.range(0, BACKENDS_USED.length)
                .mapToObj(index ->
                        "\"" + BACKENDS_USED[index].getClass().getSimpleName()
                        + "\":" + labelTimes[index])
                .collect(Collectors.joining(","));

        // Escape strings in predicate
        String escapedPredicate = predicate.getPredicate().replaceAll("\"", "\\\\\\\"");

        // Concatenate everything into one Json Object
        String sourceEntry = (isSourceDefined)
                ? "\"source\":\"" + source + "\","
                : "";
        String json = "{"
                      + "\"predicate\":\"" + escapedPredicate + "\","
                      + sourceEntry
                      + "\"timings\":{" + timings + "}"
                      + "}";

        return json;
    }

    @Override
    public Path getDataSource(Path dbFile) throws IOException {
        return loadSamples(dbFile)
                .map(TrainingSample::getSourceFile)
                .findFirst().get();
    }

    public static class PredicateDbIterator
            implements Iterator<TrainingSample<BPredicate, DecisionTimings>> {

        private final JsonReader reader;
        private boolean hasEnded;
        private final Map<String, Backend> backendKeyMap;

        public PredicateDbIterator(JsonReader reader) throws IOException {
            this.reader = reader;
            this.hasEnded = false;

            // Map from Key to Backend
            backendKeyMap = new HashMap<>();
            for (int i = 0; i < BACKENDS_USED.length; i++) {
                Backend b = BACKENDS_USED[i];
                backendKeyMap.put(b.getClass().getSimpleName(), b);
            }

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
        public TrainingSample<BPredicate, DecisionTimings> next() {
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

        public TrainingSample<BPredicate, DecisionTimings> translateToDbSample(Map<String, String> sampleData) {
            BPredicate pred = new BPredicate(sampleData.get("predicate"));
            Path source = null;
            if (sampleData.containsKey("source")) {
                source = Paths.get(sampleData.get("source"));
            }

            DecisionTimings l = translateTimings(sampleData.get("timings"));

            return new TrainingSample<>(pred, l, source);
        }

        public DecisionTimings translateTimings(String timings) {
            String[] solverTimes = timings.split(",");

            // Construct timing map
            Map<Backend, Double> timingMap = new HashMap<>();

            for (int i = 0; i < solverTimes.length; i++) {
                String[] timeSplit = solverTimes[i].split(":");
                String backendKey = timeSplit[0];
                Double time = Double.parseDouble(timeSplit[1]);
                timingMap.put(backendKeyMap.get(backendKey), time);
            }
            return new DecisionTimings((BPredicate) null, timingMap, BACKENDS_USED);
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
                // Prepare data map
                Map<Backend, Double> timingMap = new HashMap<>();

                // Read entries
                reader.beginObject();
                while (JsonToken.NAME.equals(reader.peek())) {
                    String backendKey = reader.nextName();
                    Double time = reader.nextDouble();
                    timingMap.put(backendKeyMap.get(backendKey), time);
                }
                reader.endObject();

                // Create ordered string
                String solverString = Arrays.stream(BACKENDS_USED)
                        .map(b ->
                                b.getClass().getSimpleName()
                                + ":" + timingMap.getOrDefault(b, -1.))
                        .collect(Collectors.joining(","));

                data.put("timings", solverString);
            } else {
                log.warn("Unknown property \"{}\"", property);
                reader.skipValue();
            }
        }
    }

}
