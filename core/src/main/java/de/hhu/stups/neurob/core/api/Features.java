package de.hhu.stups.neurob.core.api;

public interface Features {

    /**
     * @return The number of entries this feature's vector has.
     */
    int getFeatureDimension();

    Double[] getFeatureArray();

}
