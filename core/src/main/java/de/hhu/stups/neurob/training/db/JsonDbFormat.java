package de.hhu.stups.neurob.training.db;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.PredicateLabelGenerating;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;
import de.prob.cli.CliVersionNumber;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JsonDbFormat implements PredicateDbFormat<PredDbEntry> {

    /**
     * Array over backends in use, also providing an ordering by id.
     * <p>
     * The order used is the one stated for {@link PredDbEntry#DEFAULT_BACKENDS PredDbEntry}.
     */
    public final static Backend[] DEFAULT_BACKENDS = PredDbEntry.DEFAULT_BACKENDS;

    private final Backend[] BACKENDS_USED;

    private static final Logger log =
            LoggerFactory.getLogger(JsonDbFormat.class);

    /**
     * Sets the format up with the
     * {@link PredDbEntry#DEFAULT_BACKENDS default backends}.
     */
    public JsonDbFormat() {
        this(DEFAULT_BACKENDS);
    }

    /**
     * Sets the format up with the given backends to be used.
     *
     * @param backendsUsed
     */
    public JsonDbFormat(Backend[] backendsUsed) {
        this.BACKENDS_USED = backendsUsed;
    }

    /**
     * Returns the backends used by this format.
     *
     * @return
     */
    public Backend[] getBackendsUsed() {
        return BACKENDS_USED;
    }

    /**
     * Returns a label generator that generates labels conforming this format's
     * backends.
     *
     * @return
     */
    public PredicateLabelGenerating<PredDbEntry> getLabelGenerator() {
        return new PredDbEntry.Generator(3, BACKENDS_USED);
    }

    /**
     * Returns a label generator that generates labels associated with the given
     * backends.
     *
     * @param backends
     *
     * @return
     */
    public static PredicateLabelGenerating<PredDbEntry> getLabelGenerator(Backend[] backends) {
        return new PredDbEntry.Generator(3, backends);
    }

    /**
     * Returns a label generator that generates labels associated with the given
     * backends and marks them as run with the specified CLI version.
     *
     * @param backends
     *
     * @return
     */
    public PredicateLabelGenerating<PredDbEntry> getLabelGenerator(
            CliVersionNumber version, Backend... backends) {
        return new PredDbEntry.Generator(3, version, backends);
    }

    @Override
    public Stream<TrainingSample<BPredicate, PredDbEntry>> loadSamples(Path sourceFile)
            throws IOException {
        JsonReader reader = new JsonReader(Files.newBufferedReader(sourceFile));

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new PredicateDbIterator(reader, BACKENDS_USED),
                        0),
                false)
                .filter(Objects::nonNull)
                .onClose(() -> {
                    try {
                        reader.close();
                        log.trace("Closed access to {}", sourceFile);
                    } catch (IOException e) {
                        log.warn("Unable to close access to {}", sourceFile, e);
                    }
                });
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public Boolean isValidFile(Path file) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(file))) {
            return isValidJsonDb(reader);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isValidJsonDb(JsonReader reader) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement parseResult = parser.parse(reader);
            return parseResult.isJsonObject();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public DataGenerationStats writeSamples(TrainingData<BPredicate, PredDbEntry> trainingData, Path targetDirectory) {
        Path sourceFile = trainingData.getSourceFile();
        Path targetFile = getTargetLocation(sourceFile, targetDirectory);

        DataGenerationStats stats = new DataGenerationStats();

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
        writer.write("{");
        DataGenerationStats stats = addEntryToOpenWriter(trainingData, writer);
        writer.write("}");
        writer.flush();

        return stats;
    }

    public DataGenerationStats writeSamples(
            Stream<TrainingData<BPredicate, PredDbEntry>> dataStream,
            Path targetFile) {
        DataGenerationStats stats = new DataGenerationStats();

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

        log.info("Writing samples to {}", targetFile);
        try (BufferedWriter writer = Files.newBufferedWriter(targetFile)) {
            DataGenerationStats writeStats =
                    writeSamples(dataStream, writer);
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

    public DataGenerationStats writeSamples(
            Stream<TrainingData<BPredicate, PredDbEntry>> dataStream,
            Writer writer)
            throws IOException {
        // Open Json File
        writer.write("{");

        DataGenerationStats stats = new DataGenerationStats();

        dataStream.flatMap(d -> Stream.of(null, d))
                // null objects will be separators between the data
                .skip(1) // skip first null
                .forEach(d -> {
                    if (d == null) {
                        try {
                            writer.write(",");
                        } catch (IOException e) {
                            log.error("Unable to write separator", e);
                        }
                    } else {
                        try {
                            stats.mergeWith(addEntryToOpenWriter(d, writer));
                        } catch (IOException e) {
                            log.error("Unable to add training data for {}", d.getSourceFile());
                        }
                    }
                });

        // Close Json File
        writer.write("}");
        writer.flush();

        return stats;

    }

    DataGenerationStats addEntryToOpenWriter(
            TrainingData<BPredicate, PredDbEntry> trainingData,
            Writer writer)
            throws IOException {
        // Set up stats
        DataGenerationStats stats = new DataGenerationStats();

        // get machine hash
        String machineHash = getMachineHash(trainingData.getAbsoluteSourcePath());

        // Header
        writer.write("\"" + trainingData.getSourceFile() + "\":{");
        writer.write("\"sha512\":\"" + machineHash + "\",");
        MachineType machineType = MachineType.predictTypeFromLocation(trainingData.getSourceFile());
        writer.write("\"formalism\":\"" + machineType + "\",");
        // Gathered predicates
        writer.write("\"gathered-predicates\":[");
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
        // Footer: Close gathered-predicates array in machine object
        writer.write("]}");
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

    public String translateSampleToJsonObject(TrainingSample<BPredicate, PredDbEntry> sample) {
        // Escape strings in predicate
        BPredicate predicate = sample.getData();
        String escapedPredicate = predicate.getPredicate()
                .replaceAll("\\\\", "\\\\\\\\") // Escape backslashes
                .replaceAll("\"", "\\\\\\\"") // escape quotation marks
                .replaceAll("\n", "\\\\n") // Escape line breaks
                .replaceAll("\r", "\\\\r"); // Escape line feeds

        // Setup predicate attribute
        String predAttrib = "\"predicate\":\"" + escapedPredicate + "\"";

        // Setup hash attribute
        String hash = DigestUtils.sha512Hex(predicate.getPredicate());
        String hashAttrib = "\"sha512\":\"" + hash + "\"";

        // Setup prob cli information
        String probCliAttrib = "";
        CliVersionNumber cliVersion = sample.getLabelling().getProbRevision();
        if (cliVersion != null) {
            String probVersion = cliVersion.major + "." + cliVersion.minor + "." + cliVersion.service
                                 + "-" + cliVersion.qualifier;
            String proBRevision = cliVersion.revision;
            probCliAttrib = "\"probcli\":{"
                            + "\"version\":\"" + probVersion + "\","
                            + "\"revision\":\"" + proBRevision + "\"}";
        }
        String probCliOptional = (cliVersion != null)
                ? "," + probCliAttrib
                : "";

        // Extract Backend Data
        Map<Backend, TimedAnswer> resultMap = sample.getLabelling().getResults();
        String resultString =
                Arrays.stream(BACKENDS_USED)
                        .map(backend -> translateAnswerToJson(backend, resultMap))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","));
        String resultAttrib = "\"results\":{" + resultString + "}";

        return "{" + predAttrib + "," + hashAttrib + probCliOptional + "," + resultAttrib + "}";
    }

    String translateAnswerToJson(Backend backend, Map<Backend, TimedAnswer> resultMap) {
        TimedAnswer result = resultMap.get(backend);

        if (result == null) {
            // Backend not part of the result map -> no data
            return null;
        }

        // Calculate timeout in ns
        Long timeout = backend.getTimeOutValue();
        Long timeoutNs = backend.getTimeOutUnit().toNanos(timeout);

        String answerData = "\"" + backend.getDescriptionString() + "\":{"
                            + "\"answer\":\"" + result.getAnswer().name() + "\","
                            + "\"time-in-ns\":" + result.getNanoSeconds() + ","
                            + "\"timeout-in-ns\":" + timeoutNs
                            + "}";
        return answerData;

    }

    @Override
    public DataGenerationStats copyShuffled(Path source, Path target, Random rng) throws IOException {
        Path targetFile = target.resolve("shuffled.json");
        log.info("Creating a shuffled copy of {} at {}", source, targetFile);

        log.info("Loading data from {}", source);
        List<TrainingSample<BPredicate, PredDbEntry>> samples = loadTrainingData(source)
                .flatMap(TrainingData::getSamples)
                .collect(Collectors.toList());
        log.debug("Shuffling data");
        Collections.shuffle(samples, rng);

        log.info("Writing shuffled data to {}", targetFile);
        Stream<TrainingData<BPredicate, PredDbEntry>> trainingDataStream =
                samples.stream().map(s -> new TrainingData<>(s.getSourceFile(), Stream.of(s)));

        DataGenerationStats stats = writeSamples(trainingDataStream, targetFile);

        return stats;
    }

    @Override
    public DataGenerationStats shuffleWithBuckets(Path source, int numBuckets, Path targetDir, Random rng)
            throws IOException {
        log.info("Shuffling data from {}", source);
        // Ensure target directory exists
        try {
            log.trace("Creating directory {}", targetDir);
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Could not create target directory {}",
                    targetDir, e);
            return new DataGenerationStats();
        }

        // Prepare buckets
        log.info("Splitting data into {} buckets at {}", numBuckets, targetDir);
        Path[] bucketPaths = new Path[numBuckets];
        Writer[] buckets = new Writer[numBuckets];
        final String ext = getFileExtension();
        for (int i = 0; i < numBuckets; i++) {
            bucketPaths[i] = targetDir.resolve("bucket-" + i + "." + ext);

            buckets[i] = Files.newBufferedWriter(bucketPaths[i]);
            buckets[i].write("{"); // Begin Json.
            buckets[i].flush();
        }

        splitIntoBuckets(loadTrainingData(source), buckets, rng);

        // Close buckets
        for (Writer b : buckets) {
//            b.write("\b}"); // Replace ',' at end with '}'
            b.flush();
            b.close();
        }

        // Merge buckets into single file
        Path shuffleFilePath = targetDir.resolve("shuffled." + getFileExtension());
        return mergeShuffledBuckets(shuffleFilePath, bucketPaths, rng);

    }

    void splitIntoBuckets(
            Stream<TrainingData<BPredicate, PredDbEntry>> trainingData,
            Writer[] buckets,
            Random rng) {

        trainingData
                .flatMap(TrainingData::getSamples)
                .forEach(s -> writeSampleToRandomBucket(s, buckets, rng));
    }

    void writeSampleToRandomBucket(
            TrainingSample<BPredicate, PredDbEntry> sample,
            Writer[] buckets,
            Random rng) {
        // Determine bucket.
        int b = rng.nextInt(buckets.length);
        // Need training data with single value for writing it to target bucket
        TrainingData<BPredicate, PredDbEntry> data =
                new TrainingData<>(sample.getSourceFile(), Stream.of(sample));
        try {
            addEntryToOpenWriter(data, buckets[b]);
            buckets[b].write(","); // FIXME: Will also be written after last sample, maybe leading to faulty JSON format
            buckets[b].flush();
        } catch (IOException e) {
            log.error("Unable to write {} to bucket", sample, e);
            new DataGenerationStats();
        }
    }

    /**
     * Merges data from bucket files into a single file.
     *
     * @param mergeFile Target file
     * @param bucketPaths Array of paths to buckets to be merged
     *
     * @return
     *
     * @throws IOException
     */
    DataGenerationStats mergeShuffledBuckets(Path mergeFile, Path[] bucketPaths, Random rng)
            throws IOException {
        // Collect bucket data into single file
        log.info("Shuffling data from buckets and merging them into {}", mergeFile);

        log.trace("Setting up {}", mergeFile);
        Writer shuffleFile = Files.newBufferedWriter(mergeFile);
        shuffleFile.write("{");
        shuffleFile.flush();

        DataGenerationStats stats = new DataGenerationStats();
        stats.increaseFilesCreated();
        int numBuckets = bucketPaths.length;
        for (int i = 0; i < numBuckets; i++) {
            Path b = bucketPaths[i];
            log.error("Loading bucket {}", b);
            List<TrainingSample<BPredicate, PredDbEntry>> samples =
                    loadSamples(b).collect(Collectors.toList());
            log.info("Shuffling bucket {}", b);
            Collections.shuffle(samples, rng);
            Stream<TrainingData<BPredicate, PredDbEntry>> trainingDataStream =
                    samples.stream().map(s -> new TrainingData<>(s.getSourceFile(), Stream.of(s)));

            log.error("Writing shuffled data to {}", mergeFile);
            trainingDataStream
                    .flatMap(d -> Stream.of(null, d))
                    .skip(1)
                    .forEach(d -> {
                        if (d == null) {
                            try {
                                shuffleFile.write(",");
                            } catch (IOException e) {
                                log.error("Unable to write separator", e);
                            }
                        } else {
                            try {
                                stats.mergeWith(addEntryToOpenWriter(d, shuffleFile));
                            } catch (IOException e) {
                                log.error("Unable to write {} to {}", d, mergeFile);
                            }
                        }
                    });

            if (i + 1 < numBuckets) {
                shuffleFile.write(","); // Separate contents of multiple buckets
            }
        }

        // Closing target file
        shuffleFile.write("}"); // Close Json
        shuffleFile.close();

        return stats;
    }

    @Override
    public Path getDataSource(Path dbFile) throws IOException {
        try (Stream<TrainingSample<BPredicate, PredDbEntry>> samples = loadSamples(dbFile)) {
            return samples
                    .map(TrainingSample::getSourceFile)
                    .findFirst().orElse(null);
        }
    }

    public static class PredicateDbIterator
            implements Iterator<TrainingSample<BPredicate, PredDbEntry>> {

        private final JsonReader reader;
        private final Backend[] backendsUsed;
        private boolean hasEnded;
        private boolean hasStarted;
        private final Map<String, Backend> backendKeyMap;
        private boolean alreadyCheckedForNext = false;

        /** Indicator whether we are currently in an object belonging to a specific machine */
        private boolean inMachineData;
        private Path currentSourcePath;
        private String currentHash;
        private MachineType currentFormalism;

        public PredicateDbIterator(JsonReader reader, Backend[] backendsUsed) throws IOException {
            this.reader = reader;
            this.hasEnded = false;

            this.inMachineData = false;

            this.backendsUsed = backendsUsed;

            // Map from Key to Backend
            backendKeyMap = new HashMap<>();
            for (Backend b : backendsUsed) {
                backendKeyMap.put(b.getDescriptionString(), b);
            }

            this.hasStarted = false;
        }

        @Override
        public boolean hasNext() {
            if (hasEnded) {
                return false;
            }

            if (!hasStarted) {
                try {
                    reader.beginObject();
                } catch (IOException e) {
                    log.error("Unable to read Json", e);
                    hasEnded = true;
                    return false;
                }

                hasStarted = true;
            }
            alreadyCheckedForNext = true;

            /*
             * The reader might be standing on multiple positions, depending on how much it has
             * advanced already
             *
             * (1) - outside of a machine object.
             *       Next element might be either the end '}' or a new machine object
             * (2) - inside a "gathered-predicates" list.
             *       Next element might be another predicate object or the list end ']'
             */

            // Get into machine data with gathered predicates or to end of JSON
            try {
                while (!inMachineData && !JsonToken.END_OBJECT.equals(reader.peek())) {
                    loadNextMachineData(reader);
                }

                // either in gathered-predicates within machine data object or at end of document
                if (!inMachineData) { // end of document
                    reader.endObject();
                    hasEnded = true;
                    return false;
                }

                // in gathered-predicates
                if (reader.peek().equals(JsonToken.BEGIN_OBJECT)) { // There is a predicate comming!
                    return true;
                } else {
                    // munch end of array, find end of machine data object
                    reader.endArray();
                    while (reader.peek().equals(JsonToken.NAME)) {
                        reader.nextName();
                        reader.skipValue();
                    }
                    reader.endObject();
                    inMachineData = false; // we left the machine data

                    // then check if still hasNext
                    return hasNext();
                }
            } catch (IOException e) {
                log.error("Could not properly read Json file", e);
                hasEnded = true;
                return false;
            }
        }

        private void loadNextMachineData(JsonReader json) throws IOException {
            String mch = json.nextName();
            currentSourcePath = Paths.get(mch);
            inMachineData = true;

            /*
             * Machine data is an object.
             * It contains
             * - sha512
             * - formalism
             * - gathered-predicates
             */
            json.beginObject();

            loadHashAndFormalism(json);

            // Skip to gathered-predicates
            if (inMachineData) { // might have left it during loading of hash/formalism
                boolean inGatheredPredicates = false;
                while (!inGatheredPredicates && json.peek().equals(JsonToken.NAME)) {
                    String attrib = json.nextName();

                    if (attrib.equals("gathered-predicates")) {
                        json.beginArray();
                        inGatheredPredicates = true;
                    } else {
                        json.skipValue();
                    }
                }

                // if we were unable to find gathered predicates, we can close the machine data object
                if (!inGatheredPredicates) {
                    json.endObject();
                    inMachineData = false;
                }
            }
        }

        private void loadHashAndFormalism(JsonReader json) throws IOException {
            boolean readHash = false;
            boolean readFormalism = false;

            while (!(readHash && readFormalism) && json.peek().equals(JsonToken.NAME)) {
                String attrib = json.nextName();

                if (attrib.equals("sha512")) {
                    readHash = true;
                    currentHash = json.nextString();
                } else if (attrib.equals("formalism")) {
                    readFormalism = true;
                    currentFormalism = MachineType.valueOf(json.nextString());
                } else if (attrib.equals("gathered-predicates")) {
                    // We do not want to advance the json any further
                    // FIXME: Should still read hash and formalism after preds
                    // Why is here a `break` in the first place?
                    // Due to the nature of the code, we are streaming training samples from the
                    // file. Thing is, we do not actually want to crawl through all the predicates
                    // only to get the hash or formalism to complete the first sample to return.
                    // Might employ a second reader that skips the predicates and only is
                    // responsible for reading the hash and formalism.
                    break;
                } else {
                    json.skipValue(); // we don't care about any other values
                }
            }

            // clean missing values
            if (!readHash) currentHash = null;
            if (!readFormalism) currentFormalism = null;

            // We might have reached the end of the machine data
            if (json.peek().equals(JsonToken.END_OBJECT)) {
                json.endObject();
                inMachineData = false;
            }
        }

        @Override
        public TrainingSample<BPredicate, PredDbEntry> next() {
            // Safety net; hasNext advances the JsonReader and we need that for next() to work
            if (!alreadyCheckedForNext) {
                boolean hasNext = hasNext();
                if (!hasNext) {
                    return null;
                }
            }
            alreadyCheckedForNext = false; // set variable for next call

            return readTrainingSample(reader);
        }

        TrainingSample<BPredicate, PredDbEntry> readTrainingSample(JsonReader json) {
            try {
                Map<String, Object> sampleData = nextSampleData(json);
                return translateToDbSample(sampleData);
            } catch (IOException e) {
                log.error(
                        "Could not read next sample from data base properly", e);
                hasEnded = true;
            }
            return null;
        }

        TrainingSample<BPredicate, PredDbEntry> translateToDbSample(Map<String, Object> sampleData) {
            BPredicate pred = (BPredicate) sampleData.get("predicate");

            // Collect used backends
            Map<Backend, TimedAnswer> results = new HashMap<>();
            Map<String, TimedAnswer> sampleResults = (Map<String, TimedAnswer>) sampleData.get("results");
            // TODO/FIXME: Here we are loosing data - backends not listed are discarded, which is not good.
            Arrays.stream(backendsUsed)
                    .forEach(b -> results.put(b, sampleResults.getOrDefault(
                            b.getDescriptionString(), null)));

            BMachine source = currentSourcePath != null
                    ? new BMachine(currentSourcePath)
                    : null;

            CliVersionNumber version = (CliVersionNumber) sampleData.get("probcli");
            PredDbEntry l = new PredDbEntry(pred, source, backendsUsed, results, version);

            return new TrainingSample<>(pred, l, currentSourcePath);
        }

        Map<String, Object> nextSampleData(JsonReader json) throws IOException {

            json.beginObject();
            Map<String, Object> data = new HashMap<>();
            while (!json.peek().equals(JsonToken.END_OBJECT)) {
                readSampleProperty(json, data);
            }
            json.endObject();

            // ensure results map
            if (!data.containsKey("results")) {
                data.put("results", new HashMap<>());
            }

            return data;
        }

        void readSampleProperty(JsonReader json,
                Map<String, Object> data) throws IOException {
            String property = json.nextName();

            if ("predicate".equals(property)) {
                data.put("predicate", BPredicate.of(json.nextString()));
            } else if ("sha512".equals(property)) {
                data.put("sha512", json.nextString());
            } else if ("results".equals(property)) {
                Map<String, TimedAnswer> results = readResultsMap(json);
                data.put("results", results);
            } else if ("probcli".equals(property)) {
                CliVersionNumber cliVersionNumber = readProBVersion(json);
                data.put("probcli", cliVersionNumber);
            } else {
                log.warn("Unknown property \"{}\", skipping value", property);
                json.skipValue();
            }
        }

        Map<String, TimedAnswer> readResultsMap(JsonReader json) throws IOException {
            Map<String, TimedAnswer> results = new HashMap<>();
            json.beginObject();

            while (!json.peek().equals(JsonToken.END_OBJECT)) {
                String backend = json.nextName();
                TimedAnswer answer = readTimedAnswer(json);

                results.put(backend, answer);
            }

            json.endObject();
            return results;
        }

        TimedAnswer readTimedAnswer(JsonReader json) throws IOException {
            json.beginObject();

            Long time = null;
            Long timeout; // in ns
            Answer answer = null;

            while (!json.peek().equals(JsonToken.END_OBJECT)) {
                String property = json.nextName();

                if (property.equals("time-in-ns")) {
                    time = json.nextLong();
                } else if (property.equals("timeout-in-ns")) {
                    timeout = json.nextLong();
                } else if (property.equals("answer")) {
                    answer = Answer.valueOf(json.nextString());
                } else { // unknown property
                    log.warn("Unknown property \"{}\" in results object; skipping value", property);
                    json.skipValue();
                }
            }

            json.endObject();

            // insufficient data
            if (answer == null || time == null) {
                return null;
            }

            return new TimedAnswer(answer, time);
        }

        CliVersionNumber readProBVersion(JsonReader json) throws IOException {
            json.beginObject();

            String major = null;
            String minor = null;
            String service = null;
            String qualifier = null;
            String revision = "0000000000000000000000000000000000000000";

            while (!json.peek().equals(JsonToken.END_OBJECT)) {
                String property = json.nextName();

                if ("version".equals(property)) {
                    String probcli = json.nextString();
                    // Encoding is major.minor.service-qualifier
                    // qualifier is optional
                    String[] dotSplit = probcli.split("\\.");
                    major = dotSplit[0];
                    minor = dotSplit[1];
                    service = dotSplit[2]; // might contain the -qualifier part

                    int firstMinus = probcli.indexOf("-");
                    if (firstMinus > -1) {
                        qualifier = probcli.substring(firstMinus + 1); // + 1 ignores the minus

                        service = service.split("-")[0]; // scrap -qualifier from service
                    }
                } else if ("revision".equals(property)) {
                    revision = json.nextString();
                } else { // unknown property
                    log.warn("Unknown property \"{}\" in probcli entry; skipping value", property);
                    json.skipValue();
                }
            }

            json.endObject();

            return new CliVersionNumber(major, minor, service, qualifier, revision);
        }
    }
}
