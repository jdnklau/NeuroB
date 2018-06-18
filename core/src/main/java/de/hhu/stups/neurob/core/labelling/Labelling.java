package de.hhu.stups.neurob.core.labelling;

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
