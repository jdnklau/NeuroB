package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BElement;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.testharness.TestLabelling;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DbSampleTest {

    @Test
    public void shouldWrapBElement() {
        BElement bElement = mock(BElement.class);
        Labelling labels = new TestLabelling(1., 2., 3.);

        DbSample sample = new DbSample<>(bElement, labels);

        assertEquals(bElement, sample.getBElement(),
                "Not wrapping the BElement correctly");
    }

    @Test
    public void shouldWrapLabelling() {
        BElement bElement = mock(BElement.class);
        Labelling labels = new TestLabelling(1., 2., 3.);

        DbSample sample = new DbSample<>(bElement, labels);

        assertEquals(labels, sample.getLabelling(),
                "Not wrapping labels correctly");
    }

    @Test
    public void shouldBeNullWhenNoSourceGiven() {
        BElement bElement = mock(BElement.class);
        Labelling labels = new TestLabelling(1., 2., 3.);

        DbSample sample = new DbSample<>(bElement, labels);

        assertNull(sample.getSourceMachine());
    }

    @Test
    public void shouldWrapSourceWhenGiven() {
        BElement bElement = mock(BElement.class);
        Labelling labels = new TestLabelling(1., 2., 3.);
        Path source = Paths.get("non/existent/source.mch");

        DbSample sample = new DbSample<>(bElement, labels, source);

        assertEquals(source, sample.getSourceMachine());
    }

    @Test
    public void shouldBeEqual() {
        BElement bElement = mock(BElement.class);
        Labelling labels = new TestLabelling(1., 2., 3.);
        DbSample sample1 = new DbSample<>(bElement, labels);
        DbSample sample2 = new DbSample<>(bElement, labels);

        assertEquals(sample1, sample2);
    }

    @Test
    public void shouldBeEqualWhenSourceIsGiven() {
        DbSample sample1 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 3.),
                Paths.get("non/existent/source.mch"));
        DbSample sample2 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 3.),
                Paths.get("non/existent/source.mch"));

        assertEquals(sample1, sample2);
    }

    @Test
    public void shouldBeUnequalWhenPredsDiffer() {
        DbSample sample1 = new DbSample<>(
                new BPredicate("pred1"),
                new TestLabelling(1., 2., 3.));
        DbSample sample2 = new DbSample<>(
                new BPredicate("pred2"),
                new TestLabelling(1., 2., 3.));

        assertNotEquals(sample1, sample2);
    }

    @Test
    public void shouldBeUnequalWhenLabelsDiffer() {
        DbSample sample1 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 4.));
        DbSample sample2 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 3.));

        assertNotEquals(sample1, sample2);
    }

    @Test
    public void shouldBeUnequalWhenSourcesDiffer() {
        DbSample sample1 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 3.),
                Paths.get("non/existent/source.mch"));
        DbSample sample2 = new DbSample<>(
                new BPredicate("pred"),
                new TestLabelling(1., 2., 3.),
                Paths.get("other/non/existent/source.mch"));

        assertNotEquals(sample1, sample2);

    }

}