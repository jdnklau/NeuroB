package de.hhu.stups.neurob.training.generation.util;

import de.hhu.stups.neurob.testharness.TestMachines;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PredicateCollectionIT {

    private PredicateCollection pc;
    private PredicateCollection pcEventB;

    @BeforeAll
    public void loadPredicateCollection() throws Exception {

        Api api = Main.getInjector().getInstance(Api.class);

        StateSpace ss = api.b_load(TestMachines.FORMULAE_GEN_MCH);
        pc = new PredicateCollection(ss);
        ss.kill();

        ss = api.eventb_load(TestMachines.EXAMPLE_BCM);
        pcEventB = new PredicateCollection(ss);
        ss.kill();
    }

    @Test
    public void shouldLoadInvariant() {
        List<String> invariants = new ArrayList<>();
        invariants.add("x:NATURAL");
        invariants.add("y:NAT");
        invariants.add("z:INT");
        invariants.add("(x:NATURAL) & (y:NAT) & (z:INT)");

        List<String> actual = pc.getInvariants();

        invariants.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(invariants, actual,
                "Collected invariants do not match");
    }

    @Test
    public void shouldLoadInvariantWhenEventB() {
        List<String> invariants = new ArrayList<>();
        invariants.add("x:NAT");
        invariants.add("y:NAT");
        invariants.add("x<y");
        invariants.add("y=1");
        invariants.add("z:NAT");
        invariants.add("z<2");
        invariants.add("(x:NAT) & (y:NAT) & (x<y) & (y=1) & (z:NAT) & (z<2)");

        List<String> actual = pcEventB.getInvariants();

        invariants.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(invariants, actual,
                "Collected invariants do not match");
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
    public void shouldLoadPreconditionsWhenEventB() {
        Map<String, List<String>> pres = new HashMap<>();

        // incx
        List<String> pre = new ArrayList<>();
        pre.add("z<2");
        pres.put("incZ", pre);

        assertAll("Included preconditions",
                () -> assertEquals(pres.size(), pcEventB.getPreconditions().size(),
                        "Number of preconditions does not match"),
                () -> assertEquals(pres, pcEventB.getPreconditions(),
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
    public void shouldLoadOperationNamesWithoutInitialisationIncludedWhenEventB() {
        List<String> operations = new ArrayList<>();
        operations.add("incZ");

        assertEquals(operations, pcEventB.getOperationNames(),
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
        opWeak.put("(x:NATURAL) & (y:NAT) & (z:INT)", "x=y & z<20 & (x+1:NATURAL & y:NAT & z+1:INT)");
        weakestPres.put("incx", opWeak);
        // incy
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "y<x & z<20 & y+2:NAT");
        opWeak.put("x:NATURAL", "y<x & z<20 & x:NATURAL");
        opWeak.put("z:INT", "y<x & z<20 & z+1:INT");
        opWeak.put("(x:NATURAL) & (y:NAT) & (z:INT)", "y<x & z<20 & (x:NATURAL & y+2:NAT & z+1:INT)");
        weakestPres.put("incy", opWeak);
        // sqrx
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "x<y & y:NAT");
        opWeak.put("x:NATURAL", "x<y & x*x:NATURAL");
        opWeak.put("z:INT", "x<y & z+1:INT");
        opWeak.put("(x:NATURAL) & (y:NAT) & (z:INT)", "x<y & (x*x:NATURAL & y:NAT & z+1:INT)");
        weakestPres.put("sqrx", opWeak);
        // reset
        opWeak = new HashMap<>();
        opWeak.put("y:NAT", "z>=20 or x>1000 & 1:NAT");
        opWeak.put("x:NATURAL", "z>=20 or x>1000 & 1:NATURAL");
        opWeak.put("z:INT", "z>=20 or x>1000 & 1:INT");
        opWeak.put("(x:NATURAL) & (y:NAT) & (z:INT)", "z>=20 or x>1000 & (1:NATURAL & 1:NAT & 1:INT)");
        weakestPres.put("reset", opWeak);

        assertEquals(weakestPres, pc.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadWeakestPreConditionsWhenEventB() {
        Map<String, Map<String, String>> weakestPres = new HashMap<>();
        Map<String, String> opWeak;
        opWeak = new HashMap<>();
        opWeak.put("z<2", "/* @example:grd1 */ z < 2 => z + 1 < 2");
        opWeak.put("z:NAT", "/* @example:grd1 */ z < 2 => z + 1 : NAT");
        opWeak.put("y:NAT", "/* @example:grd1 */ z < 2 => y : NAT");
        opWeak.put("x<y", "/* @example:grd1 */ z < 2 => x < y");
        opWeak.put("x:NAT", "/* @example:grd1 */ z < 2 => x : NAT");
        opWeak.put("y=1", "/* @example:grd1 */ z < 2 => y = 1");
        opWeak.put("(x:NAT) & (y:NAT) & (x<y) & (y=1) & (z:NAT) & (z<2)",
                "/* @example:grd1 */ "
                + "z < 2 => x : NAT & (y : NAT & (x < y & (y = 1 & (z + 1 : NAT "
                + "& z + 1 < 2))))");
        weakestPres.put("incZ", opWeak);

        assertEquals(weakestPres, pcEventB.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadTheoremsAsAssertionsWhenEventB() {
        List<String> assertions = new ArrayList<>();
        assertions.add("x=0");
        assertions.add("z<2");

        assertEquals(assertions, pcEventB.getAssertions(),
                "Not loaded theorems as assertions");
    }

    @Test
    public void shouldLoadBeforeAfterPredicatesWhenEventB() {
        Map<String, String> baPreds = new HashMap<>();
        baPreds.put("incZ",
                "/* @example:grd1 */ "
                + "z < 2 & z' = z + 1 & x' = x & y' = y & a' = a");

        assertEquals(baPreds, pcEventB.getBeforeAfterPredicates(),
                "Before After predicates do not match");
    }

    @Test
    public void shouldLoadPrimedInvariantsWhenEventB() {
        Map<String, String> primedInvs = new HashMap<>();
        primedInvs.put("x:NAT", "x' : NAT");
        primedInvs.put("y:NAT", "y' : NAT");
        primedInvs.put("x<y", "x' < y'");
        primedInvs.put("y=1", "y' = 1");
        primedInvs.put("z:NAT", "z' : NAT");
        primedInvs.put("z<2", "z' < 2");
        // concat of whole invariant
        primedInvs.put("(x:NAT) & (y:NAT) & (x<y) & (y=1) & (z:NAT) & (z<2)",
                "x' : NAT & (y' : NAT & (x' < y' & (y' = 1 & (z' : NAT & z' < 2))))");

        assertEquals(primedInvs, pcEventB.getPrimedInvariants(),
                "Primed invariants mismatch");
    }

    @Test
    public void shouldLoadAxiomsAsPropertiesWhenEventB() {
        List<String> properties = new ArrayList<>();
        properties.add("a:NAT");

        assertEquals(properties, pcEventB.getProperties(),
                "Axioms not correctly loaded as properties");
    }
}