package de.hhu.stups.neurob.core.api.backends;

import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
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

    @Test
    public void shouldContainPreferences() {
        BPreferences preferences = BPreferences
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .assemble();

        ProBBackend prob = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit, preferences);

        assertAll(
                () -> assertTrue(prob.getPreferences().contains("PREF1")),
                () -> assertTrue(prob.getPreferences().contains("PREF2"))
        );
    }

    @Test
    public void shouldContainPreferencesInDescription() {
        BPreferences preferences = BPreferences
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .assemble();

        ProBBackend prob = new ProBBackend(Backend.defaultTimeOut, Backend.defaultTimeUnit, preferences);

        String expected = "ProB[PREF1=value1, PREF2=value2, TIME_OUT=2500]";
        String actual = prob.getDescriptionString();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldAdjustTimeoutWhenGivenAsPreference() {
        ProBBackend prob = new ProBBackend(BPreference.set("TIME_OUT", "30000"));

        assertAll(
                () -> assertEquals(30000, prob.getTimeOutValue(),
                        "Timeout value was not adjusted"),
                () -> assertEquals(TimeUnit.MILLISECONDS, prob.getTimeOutUnit(),
                        "Time Unit was not adjusted")
        );
    }

    @Test
    public void shouldAdjustTimeoutWhenGivenAsPreferenceAndAlsoAsValues() {
        ProBBackend prob = new ProBBackend(100L, TimeUnit.SECONDS,
                BPreference.set("TIME_OUT", "30000"));

        assertAll(
                () -> assertEquals(30000, prob.getTimeOutValue(),
                        "Timeout value was not adjusted"),
                () -> assertEquals(TimeUnit.MILLISECONDS, prob.getTimeOutUnit(),
                        "Time Unit was not adjusted")
        );
    }

}
