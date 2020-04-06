package de.hhu.stups.neurob.core.labelling;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Labelling {

    protected final Double[] labellingArray;
    protected final int labellingDimension;

    public Labelling(Double... labellingArray) {
        this.labellingArray = labellingArray;
        this.labellingDimension = labellingArray.length;
    }

    /**
     * @return the Labelling in array form.
     */
    public Double[] getLabellingArray() {
        return labellingArray;
    }

    /**
     * @return Number of entries in the labelling array form.
     */
    public int getLabellingDimension() {
        return labellingDimension;
    }

    /**
     * Returns a comma separated String of the labelling array.
     *
     * @return
     */
    public String getLabellingString() {
        return String.join(",",
                Arrays.stream(getLabellingArray())
                        .map(d -> d != null ? Double.toString(d) : "null")
                        .collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return getLabellingString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Labelling) {
            return Arrays.equals(labellingArray, ((Labelling) o).labellingArray);
        }
        return false;
    }
}
