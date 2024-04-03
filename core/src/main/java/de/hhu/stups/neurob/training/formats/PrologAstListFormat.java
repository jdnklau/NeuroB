package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.features.predicates.PrologBAst;
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

public class PrologAstListFormat implements TrainingDataFormat<PrologBAst, Labelling>{
    private static final Logger log =
            LoggerFactory.getLogger(PrologAstListFormat.class);

    @Override
    public DataGenerationStats writeSamples(TrainingData<PrologBAst, Labelling> trainingData, Path targetDirectory) throws IOException {
        // get target writer
        Path targetFile = getTargetLocation(
                trainingData.getSourceFile(),
                targetDirectory);

        // Ensure target subdirectory exists
        Path targetSubdir = targetFile.getParent();

        if (!Files.exists(targetSubdir)) {
            try {
                log.trace("Creating directory {}", targetSubdir);
                Files.createDirectories(targetSubdir);
            } catch (IOException e) {
                log.error("Could not create target directory {}",
                        targetSubdir, e);
                return new DataGenerationStats();
            }
        }

        Writer out = Files.newBufferedWriter(targetFile);
        log.info("Writing to {}", targetFile);

        DataGenerationStats stats = writeSamples(trainingData, out);
        out.close();
        return stats;
    }

    public DataGenerationStats writeSamples(TrainingData<PrologBAst, Labelling> trainingData,
            Writer out) throws IOException {
        // Set up statistics
        DataGenerationStats stats = new DataGenerationStats();

        trainingData.getSamples().map(this::generateBAstLine).forEach(
                entry -> {
                    try {
                        log.debug("Writing to File: {}", entry);
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

    private String generateBAstLine(TrainingSample<PrologBAst, Labelling> sample) {
        String ast = sample.getData().getAst();
        Labelling l = sample.getLabelling();

        return l.getLabellingString() + "|" + ast;
    }

    @Override
    public Stream<TrainingSample<PrologBAst, Labelling>> loadSamples(Path sourceFile) throws IOException {
        log.error("Not yet supported");
        return null;
    }

    @Override
    public String getFileExtension() {
        return "basts";
    }
}
