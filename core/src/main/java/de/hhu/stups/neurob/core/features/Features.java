package de.hhu.stups.neurob.core.features;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface Features {

    /**
     * @return The number of entries this feature's vector has.
     */
    int getFeatureDimension();

    Double[] getFeatureArray();

    /**
     * @return String representation of the feature.
     */
    default String getFeatureString() {
        return String.join(",",
                Arrays.stream(getFeatureArray())
                .map(d -> Double.toString(d))
                .collect(Collectors.toList())
        );
    }

}
