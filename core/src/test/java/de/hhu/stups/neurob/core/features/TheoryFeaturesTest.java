package de.hhu.stups.neurob.core.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TheoryFeaturesTest {

    private static final String predicate =
            "x : NATURAL & y : INTEGER & z : NATURAL"
            + " & z < 20 & a : NAT & b : NAT1 & a < 7 & c : INT"
            + " & # y . (y < x) & ! z . (z < 15 => x > 3)";

    @Test
    public void shouldMatchExpectedFeatureArray() throws Exception {
        TheoryFeatures features = new TheoryFeatures(predicate);

        Double[] expected = new Double[]{0., 5., 1., 1., 9., 0., 0., 0., 6.,
                0., 0., 6., 4., 2., 0., 1., 0.};
        Double[] actual = features.getFeatureArray();

        assertArrayEquals(expected, actual,
                "Feature arrays do not match");
    }

    @Test
    public void shouldHaveMatchingDimensions() throws Exception {
        TheoryFeatures features = new TheoryFeatures(predicate);

        int expected = TheoryFeatures.featureDimension;
        int actual = features.getFeatureArray().length;

        assertEquals(expected, actual,
                "Feature length does not match");
    }

    @Test
    public void shouldConcatFeaturesWithCommaForStringRepresentation()
            throws Exception{
        TheoryFeatures features = new TheoryFeatures(predicate);

        String expected =
                "0.0,5.0,1.0,1.0,9.0,0.0,0.0,0.0,"
                + "6.0,0.0,0.0,6.0,4.0,2.0,0.0,1.0,0.0";
        String actual = features.toString();

        assertEquals(expected, actual,
                "String representation does not match");
    }

    @Test
    public void shouldCreateNewFeaturesWithGenerator() throws Exception {
        TheoryFeatures features = new TheoryFeatures.Generator().generate(predicate);

        Double[] expected = new Double[]{0., 5., 1., 1., 9., 0., 0., 0., 6.,
                0., 0., 6., 4., 2., 0., 1., 0.};
        Double[] actual = features.getFeatureArray();

        assertArrayEquals(expected, actual,
                "Feature arrays do not match");
    }

}