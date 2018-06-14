package de.hhu.stups.neurob.training.generation.util;

import de.hhu.stups.neurob.testharness.TestMachines;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PredicateCollectionTest {

    private PredicateCollection pc;

    @BeforeAll
    public void loadPredicateCollection() throws Exception {

        Api api = Main.getInjector().getInstance(Api.class);
        StateSpace ss = api.b_load(TestMachines.FORMULAE_GEN_MCH);

        pc = new PredicateCollection(ss);
        ss.kill();
    }

    @Test
    public void shouldLoadInvariant() {
        List<String> invariants = new ArrayList<>();
        invariants.add("x:NATURAL");
        invariants.add("y:NAT");
        invariants.add("z:INT");

        assertAll("Included invariants",
                pc.getInvariants().stream().map(inv ->
                        () -> assertTrue(invariants.contains(inv),
                                "Should contain " + inv))
        );

    }

    @Test
    public void shouldLoadPreconditions() {
        Map<String, List<String>> pres = new HashMap<>();

        // incx
        List<String> pre = new ArrayList<>();
        pre.add("x=y");
        pre.add("z<20");
        pres.put("incx", pre);
        // incy
        pre = new ArrayList<>();
        pre.add("y<x");
        pre.add("z<20");
        pres.put("incy", pre);
        // sqrx
        pre = new ArrayList<>();
        pre.add("x<y");
        pres.put("sqrx", pre);
        // reset
        pre = new ArrayList<>();
        pre.add("z>=20 or x>1000");
        pre.add("z>=20");
        pre.add("x>1000");
        pres.put("reset", pre);

        assertAll("Included preconditions",
                () -> assertEquals(pres.size(), pc.getPreconditions().size(),
                        "Number of preconditions does not match"),
                () -> assertEquals(pres, pc.getPreconditions(),
                        "Colelcted preconditions do not match")
        );
    }

    @Test
    public void shouldLoadOperationNamesWithoutInitialisationIncluded() {
        List<String> operations = new ArrayList<>();
        operations.add("incx");
        operations.add("incy");
        operations.add("sqrx");
        operations.add("reset");

        // NOTE: list equality depends on order; maybe revisit test

        assertEquals(operations, pc.getOperationNames(),
                "Not all operations included");
    }

    @Test
    public void shouldLoadProperties() {
        List<String> properties = new ArrayList<>();
        properties.add("n=1");
        properties.add("m=2*n");

        assertEquals(properties, pc.getProperties(),
                "Properties do not match");
    }

    @Test
    public void shouldLoadAssertions() {
        List<String> asserts = new ArrayList<>();
        asserts.add("y>z or x>z");
        asserts.add("y>z");
        asserts.add("x>z");

        assertEquals(asserts, pc.getAssertions(),
                "Assertions do not match");
    }

    @Test
    public void shouldLoadWeakestPreConditions() {
        Map<String, Map<String, String>> weakestPres = new HashMap<>();
        // for each operation, the weakest pre for each invariant is expected
        Map<String, String> opWeak;
        // incx
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "x=y & z<20 & y:NAT");
        opWeak.put("x:NATURAL", "x=y & z<20 & x+1:NATURAL");
        opWeak.put("z:INT", "x=y & z<20 & z+1:INT");
        weakestPres.put("incx", opWeak);
        // incy
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "y<x & z<20 & y+2:NAT");
        opWeak.put("x:NATURAL", "y<x & z<20 & x:NATURAL");
        opWeak.put("z:INT", "y<x & z<20 & z+1:INT");
        weakestPres.put("incy", opWeak);
        // sqrx
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "x<y & y:NAT");
        opWeak.put("x:NATURAL", "x<y & x*x:NATURAL");
        opWeak.put("z:INT", "x<y & z+1:INT");
        weakestPres.put("sqrx", opWeak);
        // reset
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "z>=20 or x>1000 & 1:NAT");
        opWeak.put("x:NATURAL", "z>=20 or x>1000 & 1:NATURAL");
        opWeak.put("z:INT", "z>=20 or x>1000 & 1:INT");
        weakestPres.put("reset", opWeak);

        assertEquals(weakestPres, pc.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }


    @Test
    @Disabled("Test needs to be for an EventB machine")
    public void shouldLoadTheorems() {
        fail();
    }

    @Test
    @Disabled("Test needs to be for an EventB machine")
    public void shouldLoadBeforeAfterPredicatesWhenEventBOnly() {
        fail();
    }

    @Test
    @Disabled("Test needs to be for an EventB machine")
    public void shouldLoadPrimedInvariantsWhenEventBOnly() {
        fail();
    }
}