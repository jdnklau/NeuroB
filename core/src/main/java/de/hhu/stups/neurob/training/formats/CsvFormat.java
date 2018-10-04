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

public class CsvFormat implements TrainingDataFormat<Features> {

    private static final Logger log =
            LoggerFactory.getLogger(CsvFormat.class);

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public <L extends Labelling>
    DataGenerationStats writeSamples(TrainingData<Features, L> trainingData,
            Path targetDirectory) throws IOException {
        // get target writer
        Path targetFile = getTargetLocation(trainingData.getSourceFile(),
                targetDirectory);
        Writer out = Files.newBufferedWriter(targetFile);
        log.info("Writing to {}", targetFile);

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
                        out.write(entry + "\n");
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
