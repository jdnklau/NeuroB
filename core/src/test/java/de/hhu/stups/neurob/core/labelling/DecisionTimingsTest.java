package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.exceptions.FormulaException;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.CbcSolveCommand.Solvers;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
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

    private StateSpace stateSpace;
    private Backend prob;
    private Backend kodkod;
    private Backend z3;
    private Backend smt_supported_interpreter;

    @BeforeEach
    public void mockStateSpace() {
        stateSpace = mock(StateSpace.class);
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
        DecisionTimings timings = new DecisionTimings("predicate",
                1, stateSpace, prob, z3);

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
    public void shouldReturn1AsLabellingDimensionWhenOnlyOneBackEndProvided()
            throws LabelCreationException {
        DecisionTimings timings = new DecisionTimings("predicate",
                1, stateSpace, prob);

        int expected = 1;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual,
                "Labelling dimension does not match");
    }

    @Test
    public void shouldReturn1AsSampleSizeWhen1WasSpecified()
            throws LabelCreationException {
        DecisionTimings timings = new DecisionTimings("predicate",
                1, stateSpace, prob);

        int expected = 1;
        int actual = timings.getSampleSize();

        assertEquals(expected, actual,
                "Sampling size does not match");
    }

    @Test
    public void shouldReturnPredicate() throws Exception {
        DecisionTimings timings = new DecisionTimings("predicate",
                1, stateSpace, prob);

        assertEquals("predicate", timings.getPredicate());
    }

    @Test
    public void shouldReturn3AsSampleSizeWhen3WasSpecified()
            throws LabelCreationException {
        DecisionTimings timings = new DecisionTimings("predicate",
                3, stateSpace, prob);

        int expected = 3;
        int actual = timings.getSampleSize();

        assertEquals(expected, actual,
                "Sampling size does not match");
    }

    @Test
    public void shouldReturn3AsLabellingDimensionWhenThreeBackendsAreProvided()
            throws LabelCreationException {
        DecisionTimings timings = new DecisionTimings("predicate",
                1, stateSpace, prob, kodkod, z3);

        int expected = 3;
        int actual = timings.getLabellingDimension();

        assertEquals(expected, actual,
                "Labelling dimension does not match");
    }

    @Test
    public void shouldSampleOnce() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(100L);

        DecisionTimings timings = new DecisionTimings("predicate", 1, stateSpace, prob);

        Double expected = 1.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 1ns");
    }

    @Test
    public void shouldSampleThrice() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);

        DecisionTimings timings = new DecisionTimings("predicate", 3, stateSpace, prob);

        Double expected = 3.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 3ns");
    }

    @Test
    public void shouldReturnNegativeWhenOneSamplingIsUndecidable() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(-1L) // undecidable
                .thenReturn(6L);

        DecisionTimings timings = new DecisionTimings("predicate", 3, stateSpace, prob);

        assertTrue(0 > timings.getTiming(prob),
                "Measured time should be negative");
    }

    @Test
    void shouldSampleThriceForEachBackend() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenReturn(12L)
                .thenReturn(5L);

        DecisionTimings timings = new DecisionTimings("predicate", 3, stateSpace,
                prob, kodkod);

        assertAll("Average of three runs for ProB and KodKod",
                () -> assertEquals(3., timings.getTiming(prob), 1e-5,
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(7., timings.getTiming(kodkod), 1e-5,
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    void shouldReturnArrayWithLabellingsInOrderOfBackends() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenReturn(12L)
                .thenReturn(5L);

        DecisionTimings timings = new DecisionTimings("predicate", 3, stateSpace,
                prob, kodkod);

        assertAll("Average of three runs for ProB and KodKod",
                () -> assertEquals(3., timings.getLabellingArray()[0], 1e-5,
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(7., timings.getLabellingArray()[1], 1e-5,
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    public void shouldSampleOnceWhenUsingGenerator() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(100L);

        DecisionTimings timings =
                new DecisionTimings.Generator(1, prob)
                        .generate("predicate", stateSpace);

        Double expected = 1.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 1ns");
    }

    @Test
    public void shouldSampleThriceWhenUsingGenerator() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);

        DecisionTimings timings =
                new DecisionTimings.Generator(3, prob)
                        .generate("predicate", stateSpace);

        Double expected = 3.;
        Double actual = timings.getTiming(prob);

        assertEquals(expected, actual, 1e-5,
                "Measured time should be 3ns");
    }

    @Test
    void shouldSampleThriceForEachBackendWhenUsingGenerator() throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenReturn(12L)
                .thenReturn(5L);

        DecisionTimings timings =
                new DecisionTimings.Generator(3, prob, kodkod)
                        .generate("predicate", stateSpace);

        assertAll("Average of three runs for ProB and KodKod",
                () -> assertEquals(3., timings.getTiming(prob), 1e-5,
                        "Measured time for ProB should be 3ns"),
                () -> assertEquals(7., timings.getTiming(kodkod), 1e-5,
                        "Measured time for KodKod should be 3ns")
        );
    }

    @Test
    public void shouldThrowExceptionWhenTimeOfOneBackendCannotBeMeasured()
            throws Exception {
        // Stub ProB
        when(prob.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(6L);
        // Stub KodKod
        when(kodkod.measureEvalTime("predicate", stateSpace,
                DecisionTimings.defaultTimeout, DecisionTimings.defaultTimeoutUnit))
                .thenReturn(4L)
                .thenThrow(new FormulaException())
                .thenReturn(5L);

        assertThrows(LabelCreationException.class,
                () -> new DecisionTimings("predicate", 3,
                        stateSpace, prob, kodkod),
                "Timings should not be creatable");

    }

    @Test
    public void shouldTimeoutWhenTimeoutIsShorterThanBackendTimeout()
            throws Exception {

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate("predicate", stateSpace))
                .then(invocation -> {
                    Thread.sleep(10_000); // sleep ten seconds
                    return true;
                });
        when(backend.measureEvalTime(anyString(), any(StateSpace.class)))
                .thenCallRealMethod();
        when(backend.measureEvalTime(anyString(), any(StateSpace.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(StateSpace.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(StateSpace.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        // Backend time out is 20 seconds, so it will not run into a time out
        when(backend.getTimeOutValue()).thenReturn(20L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.SECONDS);

        DecisionTimings timings = new DecisionTimings("predicate", 1,
                0L, TimeUnit.MILLISECONDS, // Labelling should timeout
                stateSpace, backend);

        assertTrue(timings.getTiming(backend) < 0,
                "Did not timeout correctly");
    }

    @Test
    public void shouldNotTimeoutWhenTimeoutIsLongerThanBackendTimeout()
            throws Exception {

        Backend backend = mock(Backend.class);
        when(backend.decidePredicate("predicate", stateSpace))
                .then(invocation -> {
                    Thread.sleep(50L); // sleep for specified milli seconds
                    return true;
                });
        when(backend.measureEvalTime(anyString(), any(StateSpace.class)))
                .thenCallRealMethod();
        when(backend.measureEvalTime(anyString(), any(StateSpace.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(StateSpace.class)))
                .thenCallRealMethod();
        when(backend.isDecidable(anyString(), any(StateSpace.class),
                anyLong(), any(TimeUnit.class)))
                .thenCallRealMethod();
        // Backend time out is 0 milliseconds, so it would run into a time out
        when(backend.getTimeOutValue()).thenReturn(0L);
        when(backend.getTimeOutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        // Has default timeout of 20 seconds
        DecisionTimings timings = new DecisionTimings("predicate", 1,
                stateSpace, backend);

        assertTrue(timings.getTiming(backend) >= 0,
                "Did not timeout correctly");
    }
}