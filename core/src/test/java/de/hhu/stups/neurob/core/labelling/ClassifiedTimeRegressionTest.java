package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.prob.animator.command.CbcSolveCommand;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassifiedTimeRegressionTest {

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
        doReturn(CbcSolveCommand.Solvers.PROB).when(prob).toCbcEnum();

        kodkod = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.KODKOD).when(kodkod).toCbcEnum();

        z3 = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.Z3).when(z3).toCbcEnum();

        smt_supported_interpreter = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.SMT_SUPPORTED_INTERPRETER)
                .when(smt_supported_interpreter).toCbcEnum();
    }

    @Test
    public void shouldUseProBAndZ3Backends() throws LabelCreationException {
        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression(BPredicate.of("predicate"), machineAccess, prob, z3);

        Backend[] usedBackends = timings.getUsedBackends();

        assertAll("Using ProB and Z3 backends",
                () -> assertEquals(CbcSolveCommand.Solvers.PROB, usedBackends[0].toCbcEnum(),
                        "ProB backend not at index 0"),
                () -> assertEquals(CbcSolveCommand.Solvers.Z3, usedBackends[1].toCbcEnum(),
                        "ProB backend not at index 0"),
                () -> assertEquals(2, usedBackends.length,
                        "Number of backends does not match"));
    }

    @Test
    public void shouldOrderLabelsAccordingToBackendOrder() {
        Map<Backend, TimedAnswer> input = new HashMap();
        input.put(new ProBBackend(), new TimedAnswer(Answer.VALID, 4L));
        input.put(new KodkodBackend(), new TimedAnswer(Answer.VALID, 3L));
        input.put(new Z3Backend(), new TimedAnswer(Answer.VALID, 2L));
        input.put(new SmtBackend(), new TimedAnswer(Answer.VALID, 1L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(BPredicate.of("pred"), input,
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
        Map<Backend, TimedAnswer> input = new HashMap();
        input.put(new ProBBackend(), new TimedAnswer(Answer.VALID, 4L));
        input.put(new KodkodBackend(), new TimedAnswer(Answer.VALID, 3L));
        input.put(new Z3Backend(), new TimedAnswer(Answer.VALID, 2L));
        input.put(new SmtBackend(), new TimedAnswer(Answer.VALID, 1L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(BPredicate.of("pred"), input,
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
        Map<Backend, TimedAnswer> input = new HashMap();
        input.put(new ProBBackend(), new TimedAnswer(Answer.VALID, 4L));
        input.put(new Z3Backend(), new TimedAnswer(Answer.VALID, 2L));
        input.put(new SmtBackend(), new TimedAnswer(Answer.VALID, 1L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression("pred", input,
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

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression("pred", backends,
                new TimedAnswer(Answer.VALID, 1L),
                new TimedAnswer(Answer.VALID, 2L),
                new TimedAnswer(Answer.VALID, 3L));

        Double[] expected = {1.0, 2.0, 3.0};
        Double[] actual = timings.getLabellingArray();

        assertAll("Matched backends",
                () -> assertArrayEquals(expected, actual,
                        "Vector does not match"),
                () -> assertEquals(1L, timings.getTiming(backends[0]).getNanoSeconds(), 1e-10,
                        "ProB timing does not match"),
                () -> assertEquals(2L, timings.getTiming(backends[1]).getNanoSeconds(), 1e-10,
                        "Kodkod timing does not match"),
                () -> assertEquals(3L, timings.getTiming(backends[2]).getNanoSeconds(), 1e-10,
                        "Z3 timing does not match"));
    }

    @Test
    public void shouldReturn1AsLabellingDimensionWhenOnlyOneBackEndProvided()
            throws LabelCreationException {
        ClassifiedTimeRegression timings = new ClassifiedTimeRegression("predicate", machineAccess, prob);

        int expected = 1;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual,
                "Labelling dimension does not match");
    }

    @Test
    public void shouldReturnPredicate() throws Exception {
        ClassifiedTimeRegression timings = new ClassifiedTimeRegression("predicate", machineAccess, prob);
        assertEquals(BPredicate.of("predicate"), timings.getPredicate());
    }

    @Test
    public void shouldReturn3AsLabellingDimensionWhenThreeBackendsAreProvided()
            throws LabelCreationException {
        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression("predicate", machineAccess, prob, kodkod, z3);

        int expected = 3;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual, "Labelling dimension does not match");
    }

    @Test
    public void shouldSampleTimeOnce() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L))
                .thenReturn(new TimedAnswer(Answer.VALID, 100L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(predicate, machineAccess, prob);

        TimedAnswer expected = new TimedAnswer(Answer.VALID, 1L);
        TimedAnswer actual = timings.getTiming(prob);

        assertEquals(expected, actual, "Measured time should be 1ns");
    }

    @Test
    void shouldSampleForEachBackend() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L))
                .thenReturn(new TimedAnswer(Answer.VALID, 6L));
        // Stub KodKod
        when(kodkod.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 4L))
                .thenReturn(new TimedAnswer(Answer.VALID, 5L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(predicate, machineAccess, prob, kodkod);

        assertAll(
                () -> assertEquals(1L, timings.getTiming(prob).getNanoSeconds().longValue(),
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(4L, timings.getTiming(kodkod).getNanoSeconds().longValue(),
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    void shouldReturnArrayWithLabellingsInOrderOfBackends() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L));
        // Stub KodKod
        when(kodkod.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 4L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(predicate, machineAccess, prob, kodkod);

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
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L))
                .thenReturn(new TimedAnswer(Answer.VALID, 100L));

        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression.Generator(prob)
                        .generate(predicate, bMachine);

        TimedAnswer expected = new TimedAnswer(Answer.VALID, 1L);
        TimedAnswer actual = timings.getTiming(prob);

        assertEquals(expected, actual, "Measured time should be 1ns");
    }

    @Test
    public void shouldSampleThriceWhenSamplingSizeIsThree() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L))
                .thenReturn(new TimedAnswer(Answer.VALID, 2L))
                .thenReturn(new TimedAnswer(Answer.VALID, 6L));

        List<ClassifiedTimeRegression> timings =
                new ClassifiedTimeRegression.Generator(prob)
                        .generateSamples(predicate, bMachine, 3);

        double[] expected = {1., 2., 6.};
        double[] actual = timings.stream().mapToDouble(d -> d.calcCost(d.getTiming(prob))).toArray();

        assertArrayEquals(expected, actual, 1e-5,
                "Measured times not equal");
    }

    @Test
    void shouldSampleThriceForEachBackendWhenUsingGenerator() throws Exception {
        BPredicate predicate = BPredicate.of("predicate");

        // Stub ProB
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 1L))
                .thenReturn(new TimedAnswer(Answer.VALID, 2L))
                .thenReturn(new TimedAnswer(Answer.VALID, 6L));
        // Stub KodKod
        when(kodkod.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 4L))
                .thenReturn(new TimedAnswer(Answer.VALID, 12L))
                .thenReturn(new TimedAnswer(Answer.VALID, 5L));

        List<ClassifiedTimeRegression> timings =
                new ClassifiedTimeRegression.Generator(prob, kodkod)
                        .generateSamples(predicate, bMachine, 3);

        double[] probExpected = {1., 2., 6.};
        double[] probActual = timings.stream().mapToDouble(d -> d.calcCost(d.getTiming(prob))).toArray();

        double[] kodkodExpected = {4., 12., 5.};
        double[] kodkodActual = timings.stream().mapToDouble(d -> d.calcCost(d.getTiming(kodkod))).toArray();

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
        when(prob.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenReturn(new TimedAnswer(Answer.VALID, 2L));
        // Stub KodKod
        when(kodkod.solvePredicate(predicate, machineAccess,
                ClassifiedTimeRegression.defaultTimeout, ClassifiedTimeRegression.defaultTimeoutUnit))
                .thenThrow(new FormulaException());

        assertThrows(LabelCreationException.class,
                () -> new ClassifiedTimeRegression(predicate, machineAccess, prob, kodkod),
                "Timings should not be creatable");

    }

    @Test
    void shouldUseTimeAsCostWhenSolvableResponse() throws LabelCreationException {
        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression(BPredicate.of("predicate"),
                        50L, TimeUnit.NANOSECONDS,
                        machineAccess, prob, z3);
        TimedAnswer valid = new TimedAnswer(Answer.VALID, 1L);
        TimedAnswer invalid = new TimedAnswer(Answer.INVALID, 1L);
        TimedAnswer solvable = new TimedAnswer(Answer.SOLVABLE, 1L);

        assertAll(
                () -> assertEquals(1L, timings.calcCost(valid).longValue()),
                () -> assertEquals(1L, timings.calcCost(invalid).longValue()),
                () -> assertEquals(1L, timings.calcCost(solvable).longValue())
        );
    }

    @Test
    void shouldAddTimeoutToCostWhenUnknownResponse() throws LabelCreationException {
        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression(BPredicate.of("predicate"),
                        50L, TimeUnit.NANOSECONDS,
                        machineAccess, prob, z3);
        TimedAnswer unknown = new TimedAnswer(Answer.UNKNOWN, 1L);

        assertEquals(51L, timings.calcCost(unknown).longValue());
    }

    @Test
    void shouldAddTimeoutToCostWhenErrorOrTimeout() throws LabelCreationException {
        ClassifiedTimeRegression timings =
                new ClassifiedTimeRegression(BPredicate.of("predicate"),
                        5L, TimeUnit.MICROSECONDS,
                        machineAccess, prob, z3);
        TimedAnswer error = new TimedAnswer(Answer.ERROR, 15L);
        TimedAnswer timeout = new TimedAnswer(Answer.TIMEOUT, 51L);

        assertAll(
                () -> assertEquals(5015L, timings.calcCost(error).longValue()),
                () -> assertEquals(5051L, timings.calcCost(timeout).longValue())
        );
    }

    @Test
    public void shouldReturnCostEstimatedLabelling() {
        Map<Backend, TimedAnswer> input = new HashMap();
        input.put(new ProBBackend(), new TimedAnswer(Answer.VALID, 4L));
        input.put(new KodkodBackend(), new TimedAnswer(Answer.VALID, 3L));
        input.put(new Z3Backend(), new TimedAnswer(Answer.VALID, 2L));
        input.put(new SmtBackend(), new TimedAnswer(Answer.VALID, 1L));

        ClassifiedTimeRegression timings = new ClassifiedTimeRegression(BPredicate.of("pred"), input,
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
    void shouldTranslatePredDbEntry() {
        Backend[] backends = {
                new ProBBackend(10, TimeUnit.MILLISECONDS),
                new KodkodBackend(10, TimeUnit.MILLISECONDS),
                new Z3Backend(10, TimeUnit.MILLISECONDS)};
        PredDbEntry dbEntry = new PredDbEntry(BPredicate.of("foo"),
                null,
                backends,
                new TimedAnswer(Answer.VALID, 5_000L),
                new TimedAnswer(Answer.UNKNOWN, 5_000L),
                new TimedAnswer(Answer.TIMEOUT, 5_000L));

        Double[] expected = {5_000., 10_005_000., 10_005_000.};
        Double[] actual = new ClassifiedTimeRegression.Translator().translate(dbEntry).getLabellingArray();

        assertArrayEquals(expected, actual);
    }

}
