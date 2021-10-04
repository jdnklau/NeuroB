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
    public void shouldNotBeEqualWhenDifferentSettings() {
        ProBBackend first = new ProBBackend(new BPreference("FOO", "BAR"));
        ProBBackend second = new ProBBackend(new BPreference("FIZ", "BAZ"));

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

        BPreferences expected = BPreferences
                .set("TIME_OUT", Long.toString(Backend.defaultTimeOut)) // Default time out
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .assemble();
        BPreferences actual = prob.getPreferences();

        assertEquals(expected, actual);
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

    @Test
    void shouldNotExplicitlySetClpfdAndSmt() {
        Backend b = new ProBBackend();

        assertAll(
                () -> assertFalse(b.isClpfdSetExplicity,
                        "Should not have Clpfd marked as explicitly set"),
                () -> assertFalse(b.isSmtSetExplicity,
                        "Should not have Smt marked as explicitly set")
                );
    }


    @Test
    void shouldNotListSmtAndClpfdAsSettingsWhenNotExplicitlySet() {
        Backend b = new ProBBackend();

        BPreferences preferences = b.getPreferences();

        assertAll(
                () -> assertFalse(preferences.contains("CLPFD"),
                        "Should not have Clpfd listed as setting"),
                () -> assertFalse(preferences.contains("SMT"),
                        "Should not have Smt listed as setting")
        );
    }

    @Test
    void shouldMarkClpfdAsExplicitlySetWhenGivenViaConstructor() {
        BPreference explicit = new BPreference("CLPFD", "TRUE");
        Backend b = new ProBBackend(explicit);

        assertAll(
                () -> assertTrue(b.isClpfdSetExplicity,
                        "Should have Clpfd marked as explicitly set")
        );
    }

    @Test
    void shouldListClpfdAsPreferenceSetWhenGivenViaConstructor() {
        BPreference explicit = new BPreference("CLPFD", "TRUE");
        Backend b = new ProBBackend(explicit);

        assertAll(
                () -> assertTrue(b.getPreferences().contains("CLPFD"),
                        "Should list Clpfd as preference")
        );
    }

    @Test
    void shouldMarkSmtAsExplicitlySetWhenGivenViaConstructor() {
        BPreference explicit = new BPreference("SMT", "TRUE");
        Backend b = new ProBBackend(explicit);

        assertAll(
                () -> assertTrue(b.isSmtSetExplicity,
                        "Should have Smt marked as explicitly set")
        );
    }

    @Test
    void shouldListSmtAsPreferenceSetWhenGivenViaConstructor() {
        BPreference explicit = new BPreference("SMT", "TRUE");
        Backend b = new ProBBackend(explicit);

        assertAll(
                () -> assertTrue(b.getPreferences().contains("SMT"),
                        "Should list Smt as preference")
        );
    }

}
