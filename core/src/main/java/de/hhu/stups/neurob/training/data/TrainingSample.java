package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;
import java.util.Arrays;

public class TrainingSample<F extends Features, L extends Labelling> {

    private final F features;
    private final L labelling;
    private final Path sourceFile;

    public TrainingSample(F features, L labelling) {
        this(features, labelling, null);
    }

    public TrainingSample(F features, L labelling, Path sourceFile) {
        this.features = features;
        this.labelling = labelling;
        this.sourceFile = sourceFile;
    }

    public F getFeatures() {
        return features;
    }

    public L getLabelling() {
        return labelling;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public String toString() {
        return "(<" + features.getFeatureString()
               + ">, <"
               + labelling.getLabellingString() + ">)";
    }

    @Override
    public boolean equals(Object sample) {
        if (sample == null) {
            return false;
        }

        if (sample.getClass().equals(this.getClass())) {
            TrainingSample trainingSample = (TrainingSample) sample;

            // Check for each value pair whether exactly one value is null
            if (features == null ^ trainingSample.features == null) {
                return false;
            }
            if (labelling == null ^ trainingSample.labelling == null) {
                return false;
            }
            if (sourceFile == null ^ trainingSample.sourceFile == null) {
                return false;
            }

            // Due to check above, if features are nonnull
            // then trainingSample.features are also nonnull
            if (features != null && labelling != null) {
                return Arrays.equals(features.getFeatureArray(),
                        trainingSample.features.getFeatureArray())
                       && Arrays.equals(features.getFeatureArray(),
                        trainingSample.features.getFeatureArray())
                       && ((sourceFile == null)
                           || sourceFile.equals(trainingSample.sourceFile));
            } else {
                return sourceFile == null
                        || sourceFile.equals(trainingSample.sourceFile);
            }
        }

        return false;
    }

}
