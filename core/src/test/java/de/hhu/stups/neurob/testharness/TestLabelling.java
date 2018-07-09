package de.hhu.stups.neurob.testharness;

import de.hhu.stups.neurob.core.labelling.Labelling;

public class TestLabelling implements Labelling {
    private final Double[] labels;

    public TestLabelling(Double... labels) {
        this.labels = labels;
    }

    @Override
    public Double[] getLabellingArray() {
        return labels;
    }

    @Override
    public int getLabellingDimension() {
        return labels.length;
    }
}
