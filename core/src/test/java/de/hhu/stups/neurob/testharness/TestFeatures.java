package de.hhu.stups.neurob.testharness;

import de.hhu.stups.neurob.core.features.Features;

public class TestFeatures extends Features {
    private final Double[] features;

    public TestFeatures(Double... features) {
        this.features = features;
    }

    @Override
    public int getFeatureDimension() {
        return features.length;
    }

    @Override
    public Double[] getFeatureArray() {
        return features;
    }
}
