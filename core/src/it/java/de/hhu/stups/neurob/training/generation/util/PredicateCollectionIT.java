package de.hhu.stups.neurob.training.generation.util;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.testharness.TestMachines;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PredicateCollectionIT {

    private PredicateCollection pc;
    private PredicateCollection pcEventB;

    @BeforeAll
    public void loadPredicateCollection() throws Exception {
        MachineAccess bMachine =
                new MachineAccess(Paths.get(TestMachines.FORMULAE_GEN_MCH));
        pc = new PredicateCollection(bMachine);
        bMachine.close();

        bMachine = new MachineAccess(Paths.get(TestMachines.EXAMPLE_BCM));
        pcEventB = new PredicateCollection(bMachine);
        bMachine.close();
    }

    @Test
    public void shouldLoadInvariant() {
        List<String> invariants = new ArrayList<>();
        invariants.add("x:NATURAL");
        invariants.add("y:NAT");
        invariants.add("z:INT");
        invariants.add("(x:NATURAL) & (y:NAT) & (z:INT)");

        List<String> actual = pc.getInvariants().stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

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

        List<String> actual = pcEventB.getInvariants().stream()
                .map(BPredicate::toString)
                .collect(Collectors.toList());

        invariants.sort(Comparator.naturalOrder());
        actual.sort(Comparator.naturalOrder());

        assertEquals(invariants, actual,
                "Collected invariants do not match");
    }

    @Test
    public void shouldLoadPreconditions() {
        Map<String, List<BPredicate>> pres = new HashMap<>();

        // incx
        List<String> pre = new ArrayList<>();
        pre.add("x=y");
        pre.add("z<20");
        pres.put("incx", pre.stream().map(BPredicate::new).collect(Collectors.toList()));
        // incy
        pre = new ArrayList<>();
        pre.add("y<x");
        pre.add("z<20");
        pres.put("incy", pre.stream().map(BPredicate::new).collect(Collectors.toList()));
        // sqrx
        pre = new ArrayList<>();
        pre.add("x<y");
        pres.put("sqrx", pre.stream().map(BPredicate::new).collect(Collectors.toList()));
        // reset
        pre = new ArrayList<>();
        pre.add("z>=20 or x>1000");
        pre.add("z>=20");
        pre.add("x>1000");
        pres.put("reset", pre.stream().map(BPredicate::new).collect(Collectors.toList()));

        assertAll("Included preconditions",
                () -> assertEquals(pres.size(), pc.getPreconditions().size(),
                        "Number of preconditions does not match"),
                () -> assertEquals(pres, pc.getPreconditions(),
                        "Colelcted preconditions do not match")
        );
    }

    @Test
    public void shouldLoadPreconditionsWhenEventB() {
        Map<String, List<BPredicate>> pres = new HashMap<>();

        // incx
        List<BPredicate> pre = new ArrayList<>();
        pre.add(BPredicate.of("z<2"));
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
        List<BPredicate> properties = new ArrayList<>();
        properties.add(BPredicate.of("n=1"));
        properties.add(BPredicate.of("m=2*n"));

        assertEquals(properties, pc.getProperties(),
                "Properties do not match");
    }

    @Test
    public void shouldLoadAssertions() {
        List<String> asserts = new ArrayList<>();
        asserts.add("y>z or x>z");
        asserts.add("y>z");
        asserts.add("x>z");

        List<BPredicate> expected = asserts.stream().map(BPredicate::new).collect(Collectors.toList());
        assertEquals(expected, pc.getAssertions(),
                "Assertions do not match");
    }

    @Test
    public void shouldLoadWeakestPreConditions() {
        Map<String, Map<BPredicate, BPredicate>> weakestPres = new HashMap<>();
        // for each operation, the weakest pre for each invariant is expected
        Map<BPredicate, BPredicate> opWeak;
        // incx
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("y:NAT"), BPredicate.of("x=y & z<20 & y:NAT"));
        opWeak.put(BPredicate.of("x:NATURAL"), BPredicate.of("x=y & z<20 & x+1:NATURAL"));
        opWeak.put(BPredicate.of("z:INT"), BPredicate.of("x=y & z<20 & z+1:INT"));
        opWeak.put(BPredicate.of("(x:NATURAL) & (y:NAT) & (z:INT)"),
                BPredicate.of("x=y & z<20 & (x+1:NATURAL & y:NAT & z+1:INT)"));
        weakestPres.put("incx", opWeak);
        // incy
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("y:NAT"), BPredicate.of("y<x & z<20 & y+2:NAT"));
        opWeak.put(BPredicate.of("x:NATURAL"), BPredicate.of("y<x & z<20 & x:NATURAL"));
        opWeak.put(BPredicate.of("z:INT"), BPredicate.of("y<x & z<20 & z+1:INT"));
        opWeak.put(BPredicate.of("(x:NATURAL) & (y:NAT) & (z:INT)"),
                BPredicate.of("y<x & z<20 & (x:NATURAL & y+2:NAT & z+1:INT)"));
        weakestPres.put("incy", opWeak);
        // sqrx
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("y:NAT"), BPredicate.of("x<y & y:NAT"));
        opWeak.put(BPredicate.of("x:NATURAL"), BPredicate.of("x<y & x*x:NATURAL"));
        opWeak.put(BPredicate.of("z:INT"), BPredicate.of("x<y & z+1:INT"));
        opWeak.put(BPredicate.of("(x:NATURAL) & (y:NAT) & (z:INT)"),
                BPredicate.of("x<y & (x*x:NATURAL & y:NAT & z+1:INT)"));
        weakestPres.put("sqrx", opWeak);
        // reset
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("y:NAT"), BPredicate.of("z>=20 or x>1000 & 1:NAT"));
        opWeak.put(BPredicate.of("x:NATURAL"), BPredicate.of("z>=20 or x>1000 & 1:NATURAL"));
        opWeak.put(BPredicate.of("z:INT"), BPredicate.of("z>=20 or x>1000 & 1:INT"));
        opWeak.put(BPredicate.of("(x:NATURAL) & (y:NAT) & (z:INT)"),
                BPredicate.of("z>=20 or x>1000 & (1:NATURAL & 1:NAT & 1:INT)"));
        weakestPres.put("reset", opWeak);

        assertEquals(weakestPres, pc.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadWeakestPreConditionsWhenEventB() {
        Map<String, Map<BPredicate, BPredicate>> weakestPres = new HashMap<>();
        Map<BPredicate, BPredicate> opWeak;
        opWeak = new HashMap<>();
        opWeak.put(BPredicate.of("z<2"), BPredicate.of("z < 2 => z + 1 < 2"));
        opWeak.put(BPredicate.of("z:NAT"), BPredicate.of("z < 2 => z + 1 : NAT"));
        opWeak.put(BPredicate.of("y:NAT"), BPredicate.of("z < 2 => y : NAT"));
        opWeak.put(BPredicate.of("x<y"), BPredicate.of("z < 2 => x < y"));
        opWeak.put(BPredicate.of("x:NAT"), BPredicate.of("z < 2 => x : NAT"));
        opWeak.put(BPredicate.of("y=1"), BPredicate.of("z < 2 => y = 1"));
        opWeak.put(BPredicate.of("(x:NAT) & (y:NAT) & (x<y) & (y=1) & (z:NAT) & (z<2)"),
                BPredicate.of("z < 2 => x : NAT & (y : NAT & (x < y & (y = 1 & (z + 1 : NAT "
                              + "& z + 1 < 2))))"));
        weakestPres.put("incZ", opWeak);

        assertEquals(weakestPres, pcEventB.getWeakestPreConditions(),
                "Weakest Preconditions do not match");
    }

    @Test
    public void shouldLoadTheoremsAsAssertionsWhenEventB() {
        List<BPredicate> assertions = new ArrayList<>();
        assertions.add(BPredicate.of("x=0"));
        assertions.add(BPredicate.of("z<2"));

        assertEquals(assertions, pcEventB.getAssertions(),
                "Not loaded theorems as assertions");
    }

    @Test
    public void shouldLoadBeforeAfterPredicatesWhenEventB() {
        Map<String, BPredicate> baPreds = new HashMap<>();
        baPreds.put("incZ",
                BPredicate.of("z < 2 & z' = z + 1 & x' = x & y' = y & a' = a"));

        assertEquals(baPreds, pcEventB.getBeforeAfterPredicates(),
                "Before After predicates do not match");
    }

    @Test
    public void shouldLoadPrimedInvariantsWhenEventB() {
        Map<BPredicate, BPredicate> primedInvs = new HashMap<>();
        primedInvs.put(BPredicate.of("x:NAT"), BPredicate.of("x' : NAT"));
        primedInvs.put(BPredicate.of("y:NAT"), BPredicate.of("y' : NAT"));
        primedInvs.put(BPredicate.of("x<y"), BPredicate.of("x' < y'"));
        primedInvs.put(BPredicate.of("y=1"), BPredicate.of("y' = 1"));
        primedInvs.put(BPredicate.of("z:NAT"), BPredicate.of("z' : NAT"));
        primedInvs.put(BPredicate.of("z<2"), BPredicate.of("z' < 2"));
        // concat of whole invariant
        primedInvs.put(BPredicate.of("(x:NAT) & (y:NAT) & (x<y) & (y=1) & (z:NAT) & (z<2)"),
                BPredicate.of("x' : NAT & (y' : NAT & (x' < y' & (y' = 1 & (z' : NAT & z' < 2))))"));

        assertEquals(primedInvs, pcEventB.getPrimedInvariants(),
                "Primed invariants mismatch");
    }

    @Test
    public void shouldLoadAxiomsAsPropertiesWhenEventB() {
        List<BPredicate> properties = new ArrayList<>();
        properties.add(BPredicate.of("a:NAT"));

        assertEquals(properties, pcEventB.getProperties(),
                "Axioms not correctly loaded as properties");
    }
}
