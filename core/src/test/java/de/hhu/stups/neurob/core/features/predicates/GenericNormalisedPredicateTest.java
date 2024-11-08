package de.hhu.stups.neurob.core.features.predicates;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericNormalisedPredicateTest {

    @Test
    void shouldNormaliseAllIdentifiers() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("x>y & y>x");

        String expected = "idn>idn & idn>idn";
        String actual = new GenericNormalisedPredicate.Generator().generate(pred).getPredicate().toString();

        assertEquals(expected,actual);
    }

    @Test
    void shouldNormalisePrimedIdentifiers() throws FeatureCreationException {
//        BPredicate pred = BPredicate.of("x:NAT & x'=x+1 => x' : NAT");
//        BPredicate pred = BPredicate.of("level : NAT & level′ = level+1 => level′:NAT");
        BPredicate pred = BPredicate.of("x:NAT & x$0=x+1 => x$0 : NAT");

        String expected = "idn:NAT & idn=idn+1 => idn:NAT";
        String actual = new GenericNormalisedPredicate.Generator().generate(pred).getPredicate().toString();

        assertEquals(expected,actual);
    }

    @Test
    void shouldNormalisePrimedIdentifiers2() throws FeatureCreationException {
        BPredicate pred = BPredicate.of("level : NAT & level′ = level+1 => level′:NAT");

        String expected = "idn:NAT & idn=idn+1 => idn:NAT";
        String actual = new GenericNormalisedPredicate.Generator().generate(pred).getPredicate().toString();

        assertEquals(expected,actual);
    }
}
