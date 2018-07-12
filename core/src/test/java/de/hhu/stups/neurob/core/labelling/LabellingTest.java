package de.hhu.stups.neurob.core.labelling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabellingTest {

    @Test
    public void shouldReturnInputAsArray() {
        Labelling l = new Labelling(1., 2., 3.);

        Double[] expected = {1., 2., 3.};

        assertArrayEquals(expected, l.getLabellingArray());
    }

    @Test
    public void shouldReturnEmptyArrayWhenInstantiatedWithoutArguments() {
        Labelling l = new Labelling();

        Double[] expected = {};

        assertArrayEquals(expected, l.getLabellingArray());
    }

    @Test
    public void shouldReturnDimensionOf3() {
        Labelling l = new Labelling(1., 2., 3.);

        int expected = 3;

        assertEquals(expected, l.getLabellingDimension());
    }

    @Test
    public void shouldReturnDimensionOf4() {
        Labelling l = new Labelling(1., 2., 3., 4.);

        int expected = 4;

        assertEquals(expected, l.getLabellingDimension());
    }

    @Test
    public void shouldReturnDimensionOf0WhenNoArgumentsGiven() {
        Labelling l = new Labelling();

        int expected = 0;

        assertEquals(expected, l.getLabellingDimension());
    }

    @Test
    public void shouldCommaSeparateInstantiationArguments() {
        Labelling l = new Labelling(1., 2., 3., 4.);

        String expected = "1.0,2.0,3.0,4.0";

        assertEquals(expected, l.getLabellingString());
    }

    @Test
    public void shouldBeEmptyStringWhenNoInstantiationArguments() {
        Labelling l = new Labelling();

        String expected = "";

        assertEquals(expected, l.getLabellingString());
    }
}