package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalisedPredicateTest {

    @Test
    void shouldNormaliseAllIdentifiers() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("x>y & y>x & z=5");

        String expected = "id0>id1 & id1>id0 & id2=5";
        String actual = new NormalisedPredicate.Generator().generate(pred).getPredicate().toString();

        assertEquals(expected,actual);
    }

    @Test
    void shouldNormaliseAstCleanupWithComment() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("# /* LET */ (x).(x=y) & y>2");

        String expected = "#(id0).(id0=id1) & id1>2";
        String actual = new NormalisedPredicate.Generator().generate(pred).getPredicate().toString();

        assertEquals(expected,actual);
    }
}
