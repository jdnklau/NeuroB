package de.hhu.stups.neurob.core.api.bmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BPredicateTest {

    @Test
    public void shouldReturnPredicateFromInstantiation() {
        BPredicate p = new BPredicate("pred");

        assertEquals("pred", p.getPredicate());
    }

    @Test
    public void shouldReturnNullWhenInstantiatedWithNull() {
        BPredicate p = new BPredicate(null);

        assertEquals("", p.getPredicate());
    }

    @Test
    void shouldBeEqual() {
        assertEquals(new BPredicate("pred"), new BPredicate("pred"));
    }

    @Test
    void shouldBeEqualWhenNoPred() {
        assertEquals(new BPredicate(null), new BPredicate(null));
    }

    @Test
    void shouldBeUnequal() {
        assertNotEquals(new BPredicate("pred1"), new BPredicate("pred2"));
    }

    @Test
    void shouldBeEqualToString() {
        String rawPred = "pred";
        BPredicate pred = BPredicate.of(rawPred);

        assertEquals(pred, rawPred);
    }

}
