package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.data.BData;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Features implements BData {

    protected final Double[] features;
    protected final int featureDimension;

    public Features(Double... features) {
        this.features = features;
        this.featureDimension = features.length;
    }

    /**
     * @return The number of entries this feature's vector has.
     */
    public int getFeatureDimension() {
        return featureDimension;
    }

    public Double[] getFeatureArray() {
        return features;
    }

    /**
     * @return Comma separated String of the feature values.
     */
    public String getFeatureString() {
        return String.join(",",
                Arrays.stream(getFeatureArray())
                        .map(d -> Double.toString(d))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return getFeatureString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Features) {
            Features other = (Features) o;
            // TODO: Check if unboxing (double) is needed for comparison with epsilon
            return Arrays.equals(features, other.getFeatureArray());
        }

        return false;
    }
}
