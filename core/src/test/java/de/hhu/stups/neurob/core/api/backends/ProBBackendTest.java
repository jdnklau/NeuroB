package de.hhu.stups.neurob.core.api.backends;

import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProBBackendTest {

    @Test
    public void shouldReturnProBEnum() {
        Backend prob = new ProBBackend();

        assertEquals(CbcSolveCommand.Solvers.PROB, prob.toCbcEnum());
    }

    @Test
    public void shouldBeEqualWhenSameTimeoutSettings() {
        ProBBackend first = new ProBBackend();
        ProBBackend second = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit);

        assertEquals(second, first,
                "Backends are not equal");
    }

    @Test
    public void shouldNotBeEqualWhenDifferentTimeoutSettings() {
        ProBBackend first = new ProBBackend(Backend.defaultTimeOut, TimeUnit.SECONDS);
        ProBBackend second = new ProBBackend(Backend.defaultTimeOut, TimeUnit.HOURS);

        assertNotEquals(second, first,
                "Backends should not be equal");
    }

    @Test
    public void shouldNotBeEqualWithDifferentBackend() {
        ProBBackend prob = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit);
        Backend other = mock(Backend.class);
        when(other.getTimeOutValue()).thenReturn(Backend.defaultTimeOut);
        when(other.getTimeOutUnit()).thenReturn(Backend.defaultTimeUnit);

        assertFalse(prob.equals(other),
                "Backends should not be equal");
    }

}
