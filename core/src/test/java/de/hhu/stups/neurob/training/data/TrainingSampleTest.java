package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TrainingSampleTest {

    /**
     * Creates a {@link Features} instance that is not coupled to any specific
     * implementation.
     *
     * @param features
     *
     * @return
     */
    private Features createFeature(Double... features) {
        return new Features() {
            @Override
            public int getFeatureDimension() {
                return features.length;
            }

            @Override
            public Double[] getFeatureArray() {
                return features;
            }
        };
    }

    /**
     * Creates a {@link Labelling} instance that is not coupled to any specific
     * implementation.
     *
     * @param labels
     *
     * @return
     */
    private Labelling createLabelling(Double... labels) {
        return new Labelling() {
            @Override
            public Double[] getLabellingArray() {
                return labels;
            }

            @Override
            public int getLabellingDimension() {
                return labels.length;
            }
        };
    }

    @Test
    public void shouldBeEqualWhenComparedToItself() {
        Features f = createFeature(1., 1.);
        Labelling l = createLabelling(1.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(f, l);

        assertTrue((sample.equals(sample)),
                "Not equal to itself");
    }

    @Test
    public void shouldBeEqualWhenSameFeaturesAndLabels() {
        Features f = createFeature(1., 1.);
        Labelling l = createLabelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f, l);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f, l);

        assertTrue((first.equals(second)),
                "Not equal");

    }

    @Test
    public void shouldBeEqualWithEqualFeaturesAndLabelsAndNoPath() {
        Features f1 = createFeature(1., 1.);
        Labelling l1 = createLabelling(1.);

        Features f2 = createFeature(1., 1.);
        Labelling l2 = createLabelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2);

        assertTrue((first.equals(second)),
                "Not equal");
    }

    @Test
    public void shouldBeUnequalWhenDifferentFeatures() {
        Features f1 = createFeature(1., 1.);
        Labelling l1 = createLabelling(1.);

        Features f2 = createFeature(2., 2.);
        Labelling l2 = createLabelling(2.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2);

        assertFalse((first.equals(second)),
                "Not unequal");
    }

    @Test
    public void shouldBeUnequalWhenOnlySourceDiffers() {
        Features f1 = createFeature(1., 1.);
        Labelling l1 = createLabelling(1.);

        Features f2 = createFeature(1., 1.);
        Labelling l2 = createLabelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1, Paths.get("first/path"));
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2, Paths.get("second/path"));

        assertFalse((first.equals(second)),
                "Not unequal");
    }

    @Test
    public void shouldBeEqualWhenAllInputIsEqualEvenTheSource() {
        Features f1 = createFeature(1., 1.);
        Labelling l1 = createLabelling(1.);

        Features f2 = createFeature(1., 1.);
        Labelling l2 = createLabelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1, Paths.get("first/path"));
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2, Paths.get("first/path"));

        assertTrue((first.equals(second)),
                "Not equal");

    }

    @Test
    public void shouldEqualWhenNullAsFeaturesAndLabelling() {
        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(null, null);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(null, null);

        assertTrue((first.equals(second)),
                "Not equal");
    }

}