package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvFormat implements TrainingDataFormat<Features> {
    private Writer writer;

    private static final Logger log =
            LoggerFactory.getLogger(CsvFormat.class);

    public CsvFormat() {
        this(null);
    }

    /**
     * Instantiates a CsvFormat that writes to the specified writer.
     *
     * @param writer
     */
    public CsvFormat(Writer writer) {
        this.writer = writer;
    }

    @Override
    public Path getTargetLocation(Path sourceFile, Path targetDirectory) {
        String sourceName = sourceFile.toString();
        int extensionPos = sourceName.lastIndexOf('.');
        String extensionless = sourceName.substring(0, extensionPos);

        return targetDirectory.resolve(extensionless + ".csv");
    }

    @Override
    public <L extends Labelling>
    void writeSamples(TrainingData<Features, L> trainingData,
            Path targetDirectory) throws IOException {

        // get target writer
        Writer out;
        if (this.writer != null) {
            out = this.writer;
        } else {
            Path targetFile = getTargetLocation(trainingData.getSourceFile(),
                    targetDirectory);
            out = Files.newBufferedWriter(targetFile);
            log.info("Writing to {}", targetFile);
        }

        trainingData.getSamples().map(this::generateCsvEntry).forEach(
                entry -> {
                    try {
                        out.write(entry + "\n");
                    } catch (IOException e) {
                        log.warn("Could not add entry {}",
                                entry);
                    }
                });

    }

    public String generateCsvEntry(TrainingSample sample) {
        Features f = sample.getFeatures();
        Labelling l = sample.getLabelling();

        return f.getFeatureString() + "," + l.getLabellingString();
    }
}
