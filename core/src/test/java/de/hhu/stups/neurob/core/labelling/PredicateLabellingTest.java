package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateLabellingTest {

    @Test
    public void shouldReturnPredicateFromInstantation() {
        PredicateLabelling l = new PredicateLabelling("predicate", 1.);

        BPredicate expected = BPredicate.of("predicate");

        assertEquals(expected, l.getPredicate());
    }

    @Test
    public void shouldReturnNullPredicateWhenInstantiatedWithNull() {
        PredicateLabelling l = new PredicateLabelling((BPredicate) null, 1.);

        assertEquals(null, l.getPredicate());
    }

}
