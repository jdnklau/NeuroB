package de.hhu.stups.neurob.core.features;

import java.awt.image.BufferedImage;

public abstract class CnnFeatures extends Features {


    abstract BufferedImage getFeatureImage();

    /**
     * Returns the total count of data points/features.
     * <p>
     * For a grayscale image this would be height*width,
     * for a colour image this would be height*width*3 (3 being the number
     * of colour channels).
     * </p>
     *
     * @return Total count of data points.
     */
    abstract int getPixelCount();
}
