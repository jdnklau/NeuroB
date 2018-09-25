package de.hhu.stups.neurob.training.migration.legacy;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDumpTest {

    @Test
    public void shouldSeparatePredicateFromEntry() {
        String entry = "1.23456789E8,2.23456789E8,3.23456789E8,-1.0:x>0";
        PredicateDump pdump = new PredicateDump(entry);

        BPredicate expected = new BPredicate("x>0");
        BPredicate actual = pdump.getPredicate();

        assertEquals(expected, actual,
                "Extracted Predicate does not match");
    }

    @Test
    public void shouldKeepPredicateAsOneWhenItContainsColonsItself() {
        String entry = "1.23456789E8,2.23456789E8,3.23456789E8,-1.0:x:NAT";
        PredicateDump pdump = new PredicateDump(entry);

        BPredicate expected = new BPredicate("x:NAT");
        BPredicate actual = pdump.getPredicate();

        assertEquals(expected, actual,
                "Extracted Predicate does not match");
    }

    @Test
    public void shouldExtractLabellingTimes() {
        String entry = "1.23456789E8,2.23456789E8,3.23456789E8,-1.0:x:NAT";
        PredicateDump pdump = new PredicateDump(entry);

        assertAll("Checking timing extraction",
                () -> assertEquals(1.23456789E8,
                        pdump.getTime(ProBBackend.class),
                        1e-10,
                        "ProB timing is off"),
                () -> assertEquals(2.23456789E8,
                        pdump.getTime(KodkodBackend.class),
                        1e-10,
                        "Kodkod timing is off"),
                () -> assertEquals(3.23456789E8,
                        pdump.getTime(Z3Backend.class),
                        1e-10,
                        "Z3 timing is off"),
                () -> assertEquals(-1.0,
                        pdump.getTime(SmtBackend.class),
                        1e-10,
                        "SMT_SUPPORTED_INTERPRETER timing is off")
        );
    }

    @Test
    public void shouldContainEntriesForEachBackend() {
        String entry = "1.23456789E8,2.23456789E8,3.23456789E8,-1.0:x:NAT";
        PredicateDump pdump = new PredicateDump(entry);

        Map<Backend, Double> timings = pdump.getTimings();

        assertAll("Checking timing extraction",
                () -> assertEquals(1.23456789E8,
                        timings.get(PredicateDump.PROB),
                        1e-10,
                        "ProB timing is off"),
                () -> assertEquals(2.23456789E8,
                        timings.get(PredicateDump.KODKOD),
                        1e-10,
                        "Kodkod timing is off"),
                () -> assertEquals(3.23456789E8,
                        timings.get(PredicateDump.Z3),
                        1e-10,
                        "Z3 timing is off"),
                () -> assertEquals(-1.0,
                        timings.get(PredicateDump.SMT),
                        1e-10,
                        "SMT_SUPPORTED_INTERPRETER timing is off")
        );
    }

    @Test
    public void shouldHaveNullSourceWhenNoSourceIsGiven() {
        String entry = "1.23456789E8,2.23456789E8,3.23456789E8,-1.0:x:NAT";
        PredicateDump pdump = new PredicateDump(entry);

        assertNull(pdump.getSource(),
                "No source should be given");
    }

}
