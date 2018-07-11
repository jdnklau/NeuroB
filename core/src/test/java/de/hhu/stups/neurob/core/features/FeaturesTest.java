package de.hhu.stups.neurob.core.features;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeaturesTest {

    @Test
    public void shouldReturnInputVector() {
        Double[] vector = {1., 2., 3.};
        Features f = new Features(vector);

        assertArrayEquals(vector, f.getFeatureArray(),
                "Entries do not match");
    }

    @Test
    public void shouldReturnInputAsArrayWhenUsingVariableArgumentConstructor() {
        Features f = new Features(1., 2., 3.);

        Double[] expected = {1., 2., 3.};

        assertArrayEquals(expected, f.getFeatureArray(),
                "Entries do not match");
    }

    @Test
    public void shouldReturnEmptyArrayWhenNoArgumentsWhereGiven() {
        Features f = new Features();

        Double[] expected = {};

        assertArrayEquals(expected, f.getFeatureArray(),
                "Expected an empty Double[] array");
    }

    @Test
    public void shouldReturnDimensionOf3WhenThreeArgumentsAreGiven() {
        Features f = new Features(1., 2., 3.);

        int expected = 3;

        assertEquals(expected, f.getFeatureDimension());
    }

    @Test
    public void shouldReturnDimensionOf0WhenNoArgumentsAreGiven() {
        Features f = new Features();

        int expected = 0;

        assertEquals(expected, f.getFeatureDimension());
    }


    @Test
    public void shouldReturnDimensionOf4WhenFourArgumentsAreGiven() {
        Features f = new Features(1., 2., 3., 4.);

        int expected = 4;

        assertEquals(expected, f.getFeatureDimension());
    }

    @Test
    public void shouldCommaSeparateFeatureString() {
        Features f = new Features(1., 2., 3., 4.);

        String expected = "1.0,2.0,3.0,4.0";
        String actual = f.getFeatureString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnEmptyStringWhenNoConstructorArgumentsWhereGiven() {
        Features f = new Features();

        String expected = "";
        String actual = f.getFeatureString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeEqual() {
        Features f1 = new Features(1., 2., 3.);
        Features f2 = new Features(1., 2., 3.);

        assertTrue(f1.equals(f2),
                "Feature instances are not equal");
    }

    @Test
    public void shouldBeUnequal() {
        Features f1 = new Features(1., 2., 3.);
        Features f2 = new Features(4., 5., 6.);

        assertFalse(f1.equals(f2),
                "Feature instances are equal");
    }

    @Test
    public void shouldBeUnequalWhenComparedToNull() {
        Features f1 = new Features(1., 2., 3.);

        assertFalse(f1.equals(null),
                "Feature instance is equal to null");
    }


}