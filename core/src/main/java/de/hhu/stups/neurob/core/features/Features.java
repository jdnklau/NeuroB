package de.hhu.stups.neurob.core.features;

public interface Features {

    /**
     * @return The number of entries this feature's vector has.
     */
    int getFeatureDimension();

    Double[] getFeatureArray();

    @Override
    String toString();

}
