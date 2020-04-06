package de.hhu.stups.neurob.core.api.backends.preferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BPreferenceTest {

    @Test
    void shouldGetPreferenceName() {
        BPreference pref = new BPreference("NAME", "value");
        assertEquals("NAME", pref.getName());
    }

    @Test
    void shouldGetPreferenceValue() {
        BPreference pref = new BPreference("NAME", "value");
        assertEquals("value", pref.getValue());
    }

    @Test
    void shouldStringifyToNameEqualsValue() {
        BPreference pref = new BPreference("NAME", "value");
        assertEquals("NAME=value", pref.toString());
    }

    @Test
    void shouldCreateNewInstance() {
        BPreference pref1 = new BPreference("NAME", "value");
        BPreference pref2 = BPreference.set("NAME", "value");
        assertEquals(pref1, pref2);
    }

    @Test
    void shouldBeUnequal() {
        BPreference pref1 = BPreference.set("NAME", "value1");
        BPreference pref2 = BPreference.set("NAME", "value2");
        assertNotEquals(pref1, pref2);
    }
}
