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

/**
 * Format access to a CSV file for training data with
 * {@code N} features and {@code M} labels.
 * <p>
 * The file may contain an additional header line of the form
 * Feature1,Feature2,...,FeatureN,Label1,Label2,...,LabelN
 * and each row of data follows this pattern.
 */
public class CsvFormat implements TrainingDataFormat<Features> {

    private final int numFeatureEntries;
    private final int numLabelEntries;
    private final boolean hasHeaderLine;
    private final String header;

    private static final Logger log =
            LoggerFactory.getLogger(CsvFormat.class);

    /**
     * Instantiates format access with given number of feature and label entries.
     * Creates a header line upon writing and skips the first line of loaded data,
     * as a header is expected.
     *
     * @param numFeatureEntries Number of features per training sample.
     * @param numLabelEntries Number of labels per training sample.
     */
    public CsvFormat(int numFeatureEntries, int numLabelEntries) {
        this(numFeatureEntries, numLabelEntries, true);
    }

    /**
     * Instantiates format access with given number of feature and label entries.
     * Whether a header line is to be used is optional.
     *
     * If a header line is to be used,
     * a header line is created first upon writing data to a target file
     * and upon reading in a CSV file, the first line is expected to be said
     * header line and thus skipped.
     *
     * @param numFeatureEntries Number of features per training sample.
     * @param numLabelEntries Number of labels per training sample.
     * @param hasHeaderLine Whether or not a header line is to be used.
     */
    public CsvFormat(int numFeatureEntries, int numLabelEntries, boolean hasHeaderLine) {
        this.numFeatureEntries = numFeatureEntries;
        this.numLabelEntries = numLabelEntries;
        this.hasHeaderLine = hasHeaderLine;

        // Build header
        if (!hasHeaderLine) {
            header = null;
        } else {
            // Feature0, Feature1, Feature2, ...
            String[] featureHeaders = new String[numFeatureEntries];
            for (int i = 0; i < numFeatureEntries; i++) {
                featureHeaders[i] = "Feature" + i;
            }

            // Label0, Label1, Label2, ...
            String[] labelHeaders = new String[numLabelEntries];
            for (int i = 0; i < numLabelEntries; i++) {
                labelHeaders[i] = "Label" + i;
            }

            // join them
            header = String.join(",", featureHeaders)
                     + "," +
                     String.join(",", labelHeaders);
        }
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    /**
     * Returns the header string belonging to this CSV format iff the format
     * is defined to have a header (see {@link #hasHeaderLine()}).
     * If no header is used, returns null.
     *
     * The format of the header is
     * Feature1,Feature2,...,FeatureN,Label1,Label2,...,LabelN.
     *
     * @return String containing the header if a header line is used, otherwise {@code null}.
     */
    public String getHeader() {
        return header;
    }

    /**
     * @return true iff this Csv format uses a header line.
     */
    public boolean hasHeaderLine() {
        return hasHeaderLine;
    }

    @Override
    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<Features, L> trainingData,
            Path targetDirectory) throws IOException {
        // get target writer
        Path targetFile = getTargetLocation(
                trainingData.getSourceFile(),
                targetDirectory);
        Writer out = Files.newBufferedWriter(targetFile);
        log.info("Writing to {}", targetFile);

        if (hasHeaderLine) {
            out.write(header);
            out.write('\n');
            out.flush();
        }

        return writeSamples(trainingData, out);
    }

    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<Features, L> trainingData,
            Writer out) throws IOException {
        // Set up statistics
        DataGenerationStats stats = new DataGenerationStats();

        trainingData.getSamples().map(this::generateCsvEntry).forEach(
                entry -> {
                    try {
                        out.write(entry);
                        out.write('\n');
                        stats.increaseSamplesWritten();
                    } catch (IOException e) {
                        log.warn("Could not add entry {}",
                                entry);
                        stats.increaseSamplesFailed();
                    }
                });
        out.flush();

        return stats;
    }

    public String generateCsvEntry(TrainingSample<Features, ? extends Labelling> sample) {
        Features f = sample.getData();
        Labelling l = sample.getLabelling();

        return f.getFeatureString() + "," + l.getLabellingString();
    }
}
