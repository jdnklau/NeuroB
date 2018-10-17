package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TheoryFeaturesTest {

    private static final BPredicate predicate = BPredicate.of(
            "x : NATURAL & y : INTEGER & z : NATURAL"
            + " & z < 20 & a : NAT & b : NAT1 & a < 7 & c : INT"
            + " & # y . (y < x) & ! z . (z < 15 => x > 3)");
    private static final Double[] expectedFeatures =
            {0., 5., 1., 1., 9., 0., 0., 0., 6., 0., 0., 6., 4., 2., 0., 1., 0.};

    @Test
    public void shouldGenerateWhenUsingPredicateConstructor() throws Exception {
        TheoryFeatures features = new TheoryFeatures(predicate);

        assertArrayEquals(expectedFeatures, features.getFeatureArray());
    }

    @Test
    public void shouldGenerateWhenUsingPredicateAndBMachineConstructor() throws Exception {
        TheoryFeatures features = new TheoryFeatures(predicate, (BMachine) null);

        assertArrayEquals(expectedFeatures, features.getFeatureArray());
    }

    @Test
    public void shouldWrapWhenUsingDoubleConstructor() {
        TheoryFeatures features = new TheoryFeatures(expectedFeatures);

        assertArrayEquals(expectedFeatures, features.getFeatureArray());
    }

    @Test
    public void shouldContainNullPredicateWhenUsingDoubleconstructor() {
        TheoryFeatures features = new TheoryFeatures(expectedFeatures);

        assertEquals(null, features.getPredicate());
    }

    @Test
    public void shouldReturnPredicateFromInstantiation() throws Exception {
        TheoryFeatures features = new TheoryFeatures(predicate);

        assertEquals(predicate, features.getPredicate());
    }

    @Test
    public void shouldReturnPredicateFromInstantiationWhenUsingGenerator()
            throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        assertEquals(predicate, features.getPredicate());
    }

    @Test
    public void shouldHave17Features() throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        int expected = 17;
        int actual = features.getFeatureDimension();

        assertEquals(expected, actual, "Feature dimension does not match");
    }

    @Test
    public void shouldReturnArrayWith17Entries() throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        int expected = 17;
        int actual = features.getFeatureArray().length;

        assertEquals(expected, actual, "Feature dimension does not match");
    }

    @Test
    public void shouldThrowExceptionWhenInputHasMoreThan17Entries() {
        Double[] eighteenEntries = new Double[18];
        assertThrows(IllegalArgumentException.class,
                () -> new TheoryFeatures(eighteenEntries));
    }

    @Test
    public void shouldThrowExceptionWhenInputHasMoreThan17EntriesAndPredicate() {
        Double[] eighteenEntries = new Double[18];
        assertThrows(IllegalArgumentException.class,
                () -> new TheoryFeatures("predicate", eighteenEntries));
    }

    @Test
    public void shouldThrowExceptionWhenInputHasLessThan17Entries() {
        Double[] eighteenEntries = new Double[16];
        assertThrows(IllegalArgumentException.class,
                () -> new TheoryFeatures(eighteenEntries));
    }

    @Test
    public void shouldThrowExceptionWhenInputHasLessThan17EntriesAndPredicate() {
        Double[] eighteenEntries = new Double[16];
        assertThrows(IllegalArgumentException.class,
                () -> new TheoryFeatures("predicate", eighteenEntries));
    }

    @Test
    public void shouldMatchExpectedFeatureArray() throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        assertArrayEquals(expectedFeatures, features.getFeatureArray(),
                "Feature arrays do not match");
    }

    @Test
    public void shouldHaveMatchingDimensions() throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        int expected = TheoryFeatures.featureDimension;
        int actual = features.getFeatureArray().length;

        assertEquals(expected, actual,
                "Feature length does not match");
    }

    @Test
    public void shouldConcatFeaturesWithCommaForStringRepresentation()
            throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        String expected =
                "0.0,5.0,1.0,1.0,9.0,0.0,0.0,0.0,"
                + "6.0,0.0,0.0,6.0,4.0,2.0,0.0,1.0,0.0";
        String actual = features.toString();

        assertEquals(expected, actual,
                "String representation does not match");
    }

    @Test
    public void shouldCreateNewFeaturesWithStaticGenerator() throws Exception {
        TheoryFeatures features = TheoryFeatures.generate(predicate);

        Double[] expected = new Double[]{0., 5., 1., 1., 9., 0., 0., 0., 6.,
                0., 0., 6., 4., 2., 0., 1., 0.};
        Double[] actual = features.getFeatureArray();

        assertArrayEquals(expected, actual,
                "Feature arrays do not match");
    }

    @Test
    public void shouldCreateNewFeaturesWithStaticGeneratorWhenBMachineIsNull()
            throws Exception {
        TheoryFeatures features = TheoryFeatures.generate(predicate, (BMachine) null);

        Double[] expected = expectedFeatures;
        Double[] actual = features.getFeatureArray();

        assertArrayEquals(expected, actual,
                "Feature arrays do not match");
    }

}
