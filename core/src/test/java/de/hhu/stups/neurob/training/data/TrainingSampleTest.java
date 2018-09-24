package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.testharness.TestFeatures;
import de.hhu.stups.neurob.testharness.TestLabelling;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TrainingSampleTest {

    @Test
    public void shouldBeEqualWhenComparedToItself() {
        Features f = new Features(1., 1.);
        Labelling l = new Labelling(1.);

        TrainingSample<Features, Labelling> sample =
                new TrainingSample<>(f, l);

        assertTrue((sample.equals(sample)),
                "Not equal to itself");
    }

    @Test
    public void shouldBeEqualWhenSameFeaturesAndLabels() {
        Features f = new Features(1., 1.);
        Labelling l = new Labelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f, l);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f, l);

        assertTrue((first.equals(second)),
                "Not equal");

    }

    @Test
    public void shouldBeEqualWithEqualFeaturesAndLabelsAndNoPath() {
        Features f1 = new Features(1., 1.);
        Labelling l1 = new Labelling(1.);

        Features f2 = new Features(1., 1.);
        Labelling l2 = new Labelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2);

        assertTrue((first.equals(second)),
                "Not equal");
    }

    @Test
    public void shouldBeUnequalWhenDifferentFeatures() {
        Features f1 = new Features(1., 1.);
        Labelling l1 = new Labelling(1.);

        Features f2 = new Features(2., 2.);
        Labelling l2 = new Labelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2);

        assertFalse((first.equals(second)),
                "Not unequal");
    }

    @Test
    public void shouldBeUnequalWhenDifferentLabels() {
        Features f1 = new Features(1., 1.);
        Labelling l1 = new Labelling(1.);

        Features f2 = new Features(1., 1.);
        Labelling l2 = new Labelling(2.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1);
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2);

        assertFalse((first.equals(second)),
                "Not unequal");
    }

    @Test
    public void shouldBeUnequalWhenOnlySourceDiffers() {
        Features f1 = new Features(1., 1.);
        Labelling l1 = new Labelling(1.);

        Features f2 = new Features(1., 1.);
        Labelling l2 = new Labelling(1.);

        TrainingSample<Features, Labelling> first =
                new TrainingSample<>(f1, l1, Paths.get("first/path"));
        TrainingSample<Features, Labelling> second =
                new TrainingSample<>(f2, l2, Paths.get("second/path"));

        assertFalse((first.equals(second)),
                "Not unequal");
    }

    @Test
    public void shouldBeEqualWhenAllInputIsEqualEvenTheSource() {
        Features f1 = new Features(1., 1.);
        Labelling l1 = new Labelling(1.);

        Features f2 = new Features(1., 1.);
        Labelling l2 = new Labelling(1.);

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
