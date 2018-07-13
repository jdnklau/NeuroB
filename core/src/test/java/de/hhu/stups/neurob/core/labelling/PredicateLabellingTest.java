package de.hhu.stups.neurob.core.labelling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateLabellingTest {

    @Test
    public void shouldReturnPredicateFromInstantation() {
        PredicateLabelling l = new PredicateLabelling("predicate", 1.);

        String expected = "predicate";

        assertEquals(expected, l.getPredicate());
    }

    @Test
    public void shouldReturnEmptyPredicateWhenInstantiatedWithNull() {
        PredicateLabelling l = new PredicateLabelling(null, 1.);

        String expected = "";

        assertEquals(expected, l.getPredicate());
    }

}