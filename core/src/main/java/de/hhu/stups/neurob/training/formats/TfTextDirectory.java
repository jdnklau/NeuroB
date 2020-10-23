package de.hhu.stups.neurob.training.formats;


import de.hhu.stups.neurob.core.features.predicates.RawPredFeature;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.generation.statistics.DataGenerationStats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A classification format that stores predicates according to Tensorflows
 * expected structure for {@code text_dataset_from_directory}.
 *
 * @see <a href="https://www.tensorflow.org/api_docs/python/tf/keras/preprocessing/text_dataset_from_directory">
 *         Tensorflow API
 *         </a>
 */
public class TfTextDirectory<L extends PredicateLabelling>
        implements TrainingDataFormat<RawPredFeature, L> {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(TfTextDirectory.class);

    public static final String EXT = "txt";

    @Override
    public DataGenerationStats writeSamples(
            TrainingData<RawPredFeature, L> trainingData,
            Path targetDirectory
    ) throws IOException {
        DataGenerationStats ds = new DataGenerationStats();
        trainingData.getSamples().forEach(sample ->
        {
           createTxtFile(sample, targetDirectory, ds);
        });
        return ds;
    }

    private void createTxtFile(
            TrainingSample<RawPredFeature, L> sample,
            Path targetDir,
            DataGenerationStats ds) {
        String label = sample.getLabelling().getLabellingString();
        Path labelDir = targetDir.resolve(label);

        try {
            Files.createDirectories(labelDir);
        } catch (IOException e) {
            ds.increaseSamplesFailed();
            log.error("Unable to create directory {}", labelDir, e);
            return;
        }

        RawPredFeature pred = sample.getData();
        Path file = labelDir.resolve("pred_" + ds.getSamplesWritten() + "." + EXT);
        try {
            BufferedWriter writer = Files.newBufferedWriter(file);
            writer.write(pred.getPred().getPredicate());
            writer.close();
            ds.increaseSamplesWritten();
            ds.increaseFilesCreated();
        } catch (IOException e) {
            log.error("Unable to write predicate {} to {}", pred, file, e);
            ds.increaseFilesWithErrors();
            ds.increaseSamplesFailed();
        }
    }

    @Override
    public Stream<TrainingSample<RawPredFeature, L>> loadSamples(Path sourceFile) throws IOException {
        log.error("Not yet supported");
        return null;
    }

    @Override
    public String getFileExtension() {
        return EXT;
    }
}
