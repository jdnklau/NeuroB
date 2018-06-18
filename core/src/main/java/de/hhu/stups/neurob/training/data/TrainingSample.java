package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.api.Labelling;

import java.nio.file.Path;

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
        return "(<" + features.toString()
               + ">, <"
               + labelling.toString() + ">)";
    }

    @Override
    public boolean equals(Object sample) {
        if (sample.getClass().equals(this.getClass())) {
            TrainingSample trainingSample = (TrainingSample) sample;
            return features.equals(trainingSample.getFeatures())
                   && labelling.equals(trainingSample.getLabelling())
                   && ((sourceFile != null)
                    ? sourceFile.equals(trainingSample.getSourceFile())
                    : trainingSample.getSourceFile() == null);
        }
        return false;
    }

}
