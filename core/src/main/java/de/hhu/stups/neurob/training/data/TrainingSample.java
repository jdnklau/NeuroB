package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.api.Features;
import de.hhu.stups.neurob.core.api.Labelling;

public class TrainingSample<F extends Features, L extends Labelling> {

    private final F features;

    private final L labelling;

    public TrainingSample(F features, L labelling) {
        this.features = features;
        this.labelling = labelling;
    }

    public F getFeatures() {
        return features;
    }

    public L getLabelling() {
        return labelling;
    }

    @Override
    public String toString() {
        return "(<" + features.toString()
               + ">, <"
               + labelling.toString() + ">)";
    }

}
