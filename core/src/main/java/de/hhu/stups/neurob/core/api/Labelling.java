package de.hhu.stups.neurob.core.api;

public interface Labelling {

    /**
     * @return the Labelling in array form.
     */
    Double[] getLabellingArray();

    /**
     * @return Number of entries in the labelling array form.
     */
    int getLabellingDimension();

}
