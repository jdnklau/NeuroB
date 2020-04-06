package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.animator.command.CbcSolveCommand.Solvers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecisionTimingsTest {

    private BMachine bMachine;
    private MachineAccess machineAccess;
    private Backend prob;
    private Backend kodkod;
    private Backend z3;
    private Backend smt_supported_interpreter;

    @BeforeEach
    public void mockBMachine() throws MachineAccessException {
        machineAccess = mock(MachineAccess.class);
        bMachine = mock(BMachine.class);
        when(bMachine.spawnMachineAccess()).thenReturn(machineAccess);
    }

    @BeforeEach
    public void mockBackEnds() {
        prob = mock(Backend.class);
        doReturn(Solvers.PROB).when(prob).toCbcEnum();

        kodkod = mock(Backend.class);
        doReturn(Solvers.KODKOD).when(kodkod).toCbcEnum();

        z3 = mock(Backend.class);
        doReturn(Solvers.Z3).when(z3).toCbcEnum();

        smt_supported_interpreter = mock(Backend.class);
        doReturn(Solvers.SMT_SUPPORTED_INTERPRETER)
                .when(smt_supported_interpreter).toCbcEnum();
    }

    @Test
    public void shouldUseProBAndZ3Backends() throws LabelCreationException {
        DecisionTimings timings =
                new DecisionTimings(BPredicate.of("predicate"), machineAccess, prob, z3);

        Backend[] usedBackends = timings.getUsedBackends();

        assertAll("Using ProB and Z3 backends",
                () -> assertEquals(Solvers.PROB, usedBackends[0].toCbcEnum(),
                        "ProB backend not at index 0"),
                () -> assertEquals(Solvers.Z3, usedBackends[1].toCbcEnum(),
                        "ProB backend not at index 0"),
                () -> assertEquals(2, usedBackends.length,
                        "Number of backends does not match"));
    }

    @Test
    public void shouldOrderLabelsAccordingToBackendOrder() {
        Map<Backend, Double> input = new HashMap();
        input.put(new ProBBackend(), 4.);
        input.put(new KodkodBackend(), 3.);
        input.put(new Z3Backend(), 2.);
        input.put(new SmtBackend(), 1.);

        DecisionTimings timings = new DecisionTimings(BPredicate.of("pred"), input,
                new SmtBackend(),
                new Z3Backend(),
                new KodkodBackend(),
                new ProBBackend());

        Double[] expected = {1., 2., 3., 4.};
        Double[] actual = timings.getLabellingArray();

        assertArrayEquals(expected, actual,
                "Backends not in expected order");
    }

    @Test
    public void shouldIgnoreUnlistedBackends() {
        Map<Backend, Double> input = new HashMap();
        input.put(new ProBBackend(), 4.);
        input.put(new KodkodBackend(), 3.);
        input.put(new Z3Backend(), 2.);
        input.put(new SmtBackend(), 1.);

        DecisionTimings timings = new DecisionTimings(BPredicate.of("pred"), input,
                new SmtBackend(),
                new Z3Backend(),
                new ProBBackend());

        Double[] expected = {1., 2., 4.};
        Double[] actual = timings.getLabellingArray();

        assertArrayEquals(expected, actual,
                "Backends not in expected order");
    }

    @Test
    public void shouldSetTimeOfUnmappedBackendsToNegativeOne() {
        Map<Backend, Double> input = new HashMap();
        input.put(new ProBBackend(), 4.);
        input.put(new Z3Backend(), 2.);
        input.put(new SmtBackend(), 1.);

        DecisionTimings timings = new DecisionTimings("pred", input,
                new SmtBackend(),
                new Z3Backend(),
                new KodkodBackend(),
                new ProBBackend());

        Double[] expected = {1., 2., -1.0, 4.};
        Double[] actual = timings.getLabellingArray();

        assertArrayEquals(expected, actual,
                "Backends not in expected order");
    }

    @Test
    public void shouldMatchBackendsAndTimingsByOrder() {
        Backend[] backends = {
                new ProBBackend(),
                new KodkodBackend(),
                new Z3Backend()
        };

        DecisionTimings timings = new DecisionTimings("pred", backends, 1.0, 2.0, 3.0);

        Double[] expected = {1.0, 2.0, 3.0};
        Double[] actual = timings.getLabellingArray();

        assertAll("Matched backends",
                () -> assertArrayEquals(expected, actual,
                        "Vector does not match"),
                () -> assertEquals(1.0, timings.getTiming(backends[0]), 1e-10,
                        "ProB timing does not match"),
                () -> assertEquals(2.0, timings.getTiming(backends[1]), 1e-10,
                        "Kodkod timing does not match"),
                () -> assertEquals(3.0, timings.getTiming(backends[2]), 1e-10,
                        "Z3 timing does not match"));
    }

    @Test
    public void shouldReturn1AsLabellingDimensionWhenOnlyOneBackEndProvided()
            throws LabelCreationException {
        DecisionTimings timings = new DecisionTimings("predicate", machineAccess, prob);

        int expected = 1;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual,
                "Labelling dimension does not match");
    }

    @Test
    public void shouldReturnPredicate() throws Exception {
        DecisionTimings timings = new DecisionTimings("predicate", machineAccess, prob);
        assertEquals(BPredicate.of("predicate"), timings.getPredicate());
    }

    @Test
    public void shouldReturn3AsLabellingDimensionWhenThreeBackendsAreProvided()
            throws LabelCreationException {
        DecisionTimings timings =
                new DecisionTimings("predicate", machineAccess, prob, kodkod, z3);

        int expected = 3;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual, "Labelling dimension does not match");
    }

    @Test
    public void shouldSampleTimeOnce() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(100L);

        DecisionTimings timings = new DecisionTimings(predicate, machineAccess, prob);

        Double expected = 1.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 1ns");
    }

    @Test
    public void shouldReturnNegativeWhenUndecidable() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(-1L) // undecidable
                .thenReturn(6L);

        DecisionTimings timings = new DecisionTimings(predicate, machineAccess, prob);

        assertTrue(0 > timings.getTiming(prob), "Measured time should be negative");
    }

    @Test
    void shouldSampleForEachBackend() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenReturn(5L);

        DecisionTimings timings = new DecisionTimings(predicate, machineAccess, prob, kodkod);

        assertAll(
                () -> assertEquals(1., timings.getTiming(prob), 1e-5,
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(4., timings.getTiming(kodkod), 1e-5,
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    void shouldReturnArrayWithLabellingsInOrderOfBackends() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L);
        // Stub KodKod
        when(kodkod.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L);

        DecisionTimings timings = new DecisionTimings(predicate, machineAccess, prob, kodkod);

        assertAll("Runtimes for ProB and KodKod",
                () -> assertEquals(1., timings.getLabellingArray()[0], 1e-5,
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(4., timings.getLabellingArray()[1], 1e-5,
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    public void shouldSampleOnceWhenUsingGenerator() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(100L);

        DecisionTimings timings =
                new DecisionTimings.Generator(prob)
                        .generate(predicate, bMachine);

        Double expected = 1.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 1ns");
    }

    @Test
    public void shouldSampleThriceWhenUsingGenerator() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);

        List<DecisionTimings> timings =
                new DecisionTimings.Generator(prob)
                        .generateSamples(predicate, bMachine, 3);

        double[] expected = {1., 2., 6.};
        double[] actual = timings.stream().mapToDouble(d -> d.getTiming(prob)).toArray();

        assertArrayEquals(expected, actual, 1e-5,
                "Measured times not equal");
    }

    @Test
    void shouldSampleThriceForEachBackendWhenUsingGenerator() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenReturn(12L)
                .thenReturn(5L);

        List<DecisionTimings> timings =
                new DecisionTimings.Generator(prob, kodkod)
                        .generateSamples(predicate, bMachine, 3);

        double[] probExpected = {1., 2., 6.};
        double[] probActual = timings.stream().mapToDouble(d -> d.getTiming(prob)).toArray();

        double[] kodkodExpected = {4., 12., 5.};
        double[] kodkodActual = timings.stream().mapToDouble(d -> d.getTiming(kodkod)).toArray();

        assertAll("Average of three runs for ProB and KodKod",
                () -> assertArrayEquals(probExpected, probActual, 1e-5,
                        "Measured timings for ProB do not match"),
                () -> assertArrayEquals(kodkodExpected, kodkodActual, 1e-5,
                        "Measured timings for KodKod do not match")
        );
    }

    @Test
    public void shouldThrowExceptionWhenTimeOfOneBackendCannotBeMeasured()
            throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(2L);
        // Stub KodKod
        when(kodkod.measureEvalTime(predicate, machineAccess,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenThrow(new FormulaException());

        assertThrows(LabelCreationException.class,
                () -> new DecisionTimings(predicate, machineAccess, prob, kodkod),
                "Timings should not be creatable");

    }

    @Test
    public void shouldTimeoutWhenTimeoutIsShorterThanBackendTimeout()
            throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, machineAccess))
                .then(invocation -> {
                    Thread.sleep(10_000); // sleep ten seconds
                    return true;
                });
        when(backend.measureEvalTime(any(BPredicate.class), any(MachineAccess.class)))
                .thenCallRealMethod();
        when(backend.measureEvalTime(any(BPredicate.class), any(MachineAccess.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(any(BPredicate.class), any(MachineAccess.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(any(BPredicate.class), any(MachineAccess.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        // Backend time out is 20 seconds, so it will not run into a time out
        when(backend.getTimeOutValue()).thenReturn(20L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        // Labelling should timeout
        DecisionTimings timings = new DecisionTimings(predicate, 0L, TimeUnit.MILLISECONDS,
                machineAccess, backend);

        assertTrue(timings.getTiming(backend) < 0,
                "Did not timeout correctly");
    }

    @Test
    public void shouldNotTimeoutWhenTimeoutIsLongerThanBackendTimeout()
            throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate(predicate, machineAccess))
                .then(invocation -> {
                    Thread.sleep(50L); // sleep for specified milli seconds
                    return true;
                });
        when(backend.measureEvalTime(anyString(), any(MachineAccess.class)))
                .thenCallRealMethod();
        when(backend.measureEvalTime(anyString(), any(MachineAccess.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(MachineAccess.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(MachineAccess.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        // Backend time out is 0 milliseconds, so it would run into a time out
        when(backend.getTimeOutValue()).thenReturn(0L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        // Has default timeout of 20 seconds
        DecisionTimings timings = new DecisionTimings(predicate, machineAccess, backend);

        assertTrue(timings.getTiming(backend) >= 0,
                "Did not timeout correctly");
    }
}
