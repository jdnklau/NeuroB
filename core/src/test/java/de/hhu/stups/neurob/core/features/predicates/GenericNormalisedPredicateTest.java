package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericNormalisedPredicateTest {

    @Test
    void shouldNormaliseAllIdentifiers() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("x>y & y>x");

        String expected = "id>id & id>id";
        String actual = new GenericNormalisedPredicate.Generator().generate(pred).getPred().getPredicate();

        assertEquals(expected,actual);
    }

    @Test
    void shouldNormalisePrimedIdentifiers() throws FeatureCreationException {
//        BPredicate pred = BPredicate.of("x:NAT & x'=x+1 => x' : NAT");
        BPredicate pred = BPredicate.of("x:NAT & x$0=x+1 => x$0 : NAT");

        String expected = "id:NAT & id=id+1 => id:NAT";
        String actual = new GenericNormalisedPredicate.Generator().generate(pred).getPred().getPredicate();

        assertEquals(expected,actual);
    }
}
