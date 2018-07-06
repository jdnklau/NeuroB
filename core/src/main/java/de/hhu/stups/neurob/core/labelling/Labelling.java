package de.hhu.stups.neurob.core.labelling;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface Labelling {

    /**
     * @return the Labelling in array form.
     */
    Double[] getLabellingArray();

    /**
     * @return Number of entries in the labelling array form.
     */
    int getLabellingDimension();

    /**
     * Returns a comma separated String of the labelling array.
     * @return
     */
    default String getLabellingString() {
        return String.join(",",
                Arrays.stream(getLabellingArray())
                        .map(d -> Double.toString(d))
                        .collect(Collectors.toList())
        );
    }

}
