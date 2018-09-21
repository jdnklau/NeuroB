package de.hhu.stups.neurob.core.features;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateFeaturesTest {

    @Test
    public void shouldReturnPredicateFromInstantiation() {
        PredicateFeatures f = new PredicateFeatures("pred", 1., 2.);

        BPredicate expected = BPredicate.of("pred");
        BPredicate actual = f.getPredicate();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnInputVector() {
        Double[] vector = {1., 2., 3.};
        PredicateFeatures f = new PredicateFeatures(vector);

        assertArrayEquals(vector, f.getFeatureArray(),
                "Entries do not match");
    }

    @Test
    public void shouldReturnInputAsArrayWhenUsingVariableArgumentConstructor() {
        PredicateFeatures f = new PredicateFeatures(1., 2., 3.);

        Double[] expected = {1., 2., 3.};

        assertArrayEquals(expected, f.getFeatureArray(),
                "Entries do not match");
    }

    @Test
    public void shouldReturnEmptyArrayWhenNoArgumentsWhereGiven() {
        PredicateFeatures f = new PredicateFeatures();

        Double[] expected = {};

        assertArrayEquals(expected, f.getFeatureArray(),
                "Expected an empty Double[] array");
    }

    @Test
    public void shouldReturnDimensionOf3WhenThreeArgumentsAreGiven() {
        PredicateFeatures f = new PredicateFeatures(1., 2., 3.);

        int expected = 3;

        assertEquals(expected, f.getFeatureDimension());
    }

    @Test
    public void shouldReturnDimensionOf0WhenNoArgumentsAreGiven() {
        PredicateFeatures f = new PredicateFeatures();

        int expected = 0;

        assertEquals(expected, f.getFeatureDimension());
    }


    @Test
    public void shouldReturnDimensionOf4WhenFourArgumentsAreGiven() {
        PredicateFeatures f = new PredicateFeatures(1., 2., 3., 4.);

        int expected = 4;

        assertEquals(expected, f.getFeatureDimension());
    }

    @Test
    public void shouldCommaSeparateFeatureString() {
        PredicateFeatures f = new PredicateFeatures(1., 2., 3., 4.);

        String expected = "1.0,2.0,3.0,4.0";
        String actual = f.getFeatureString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnEmptyStringWhenNoConstructorArgumentsWhereGiven() {
        PredicateFeatures f = new PredicateFeatures();

        String expected = "";
        String actual = f.getFeatureString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeEqual() {
        PredicateFeatures f1 = new PredicateFeatures(1., 2., 3.);
        PredicateFeatures f2 = new PredicateFeatures(1., 2., 3.);

        assertTrue(f1.equals(f2),
                "Feature instances are not equal");
    }

    @Test
    public void shouldBeUnequal() {
        PredicateFeatures f1 = new PredicateFeatures("pred", 1., 2., 3.);
        PredicateFeatures f2 = new PredicateFeatures("pred", 4., 5., 6.);

        assertFalse(f1.equals(f2),
                "Feature instances are equal");
    }

    @Test
    public void shouldBeUnequalWhenDifferentPredicates() {
        PredicateFeatures f1 = new PredicateFeatures("pred1", 1., 2., 3.);
        PredicateFeatures f2 = new PredicateFeatures("pred2", 1., 2., 3.);

        assertFalse(f1.equals(f2),
                "Feature instances are equal");
    }

    @Test
    public void shouldBeUnequalWhenComparedToNull() {
        PredicateFeatures f1 = new PredicateFeatures(1., 2., 3.);

        assertFalse(f1.equals(null),
                "Feature instance is equal to null");
    }
}
