package de.hhu.stups.neurob.core.api.backends.preferences;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BPreferencesTest {

    @Test
    void shouldContainPref1() {
        BPreferences prefs =
                new BPreferences(BPreference.set("PREF1", "value1"));
        assertTrue(prefs.contains("PREF1"));
    }

    @Test
    void shouldNotContainPref2() {
        BPreferences prefs =
                new BPreferences(BPreference.set("PREF1", "value1"));
        assertFalse(prefs.contains("PREF2"));
    }

    @Test
    void shouldGetPref1() {
        BPreference pref1 = BPreference.set("PREF1", "value1");
        BPreferences prefs =
                new BPreferences(pref1);
        assertEquals(pref1, prefs.get("PREF1"));
    }

    @Test
    void shouldGetNullWhenGettingNonContainedPreference() {
        BPreference pref1 = BPreference.set("PREF1", "value1");
        BPreferences prefs =
                new BPreferences(pref1);
        assertNull(prefs.get("PREF2"));
    }

    @Test
    void shouldGetDefaultWhenGetOrDefaultForNonContainedPreference() {
        BPreference pref1 = BPreference.set("PREF1", "value1");
        BPreferences prefs =
                new BPreferences(pref1);

        BPreference defPref = BPreference.set("DEFAULT", "valueDefault");

        assertEquals(defPref, prefs.getOrDefault("PREF2", defPref));
    }

    @Test
    void shouldNotReflectChangesInInternalMapWhenChangingExternalMap() {
        SortedMap<String, BPreference> externalMap = new TreeMap<>();
        externalMap.put("PREF1", BPreference.set("PREF1", "value1"));

        BPreferences preferences = new BPreferences(externalMap);

        // Alter external map
        externalMap.put("PREF1", BPreference.set("PREF1", "value2"));
        externalMap.put("PREF2", BPreference.set("PREF2", "value2"));

        assertAll(
                () -> assertNotEquals(externalMap.get("PREF1"), preferences.get("PREF1")),
                () -> assertFalse(preferences.contains("PREF2"))
        );
    }

    @Test
    void shouldConstructFromStringStringMap() {
        Map<String, String> stringPreferences = new HashMap<>();
        stringPreferences.put("PREF1", "value1");

        BPreferences preferences = new BPreferences(stringPreferences);

        assertAll(
                () -> assertTrue(preferences.contains("PREF1")),
                () -> assertEquals(BPreference.set("PREF1", "value1"),
                        preferences.get("PREF1"))
        );

    }

    @Test
    void shouldCreatePreferencesWithBuilder() {
        BPreferences expected = new BPreferences(
                BPreference.set("PREF1", "value1"),
                BPreference.set("PREF2", "value2"),
                BPreference.set("PREF3", "value3")
        );

        BPreferences actual = BPreferences
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .set("PREF3", "value3")
                .assemble();

        assertEquals(expected, actual);
    }

    @Test
    void shouldStreamPreferences() {
        BPreferences prefs = BPreferences
                .set("PREF3", "value3")
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .assemble();

        List<BPreference> expected = new ArrayList<>();
        expected.add(BPreference.set("PREF1", "value1"));
        expected.add(BPreference.set("PREF2", "value2"));
        expected.add(BPreference.set("PREF3", "value3"));

        List<BPreference> actual = prefs.stream().collect(Collectors.toList());

        assertEquals(expected, actual);

    }

    @Test
    void shouldBeListOfNameEqualsValueWhenCallingToString() {
        BPreferences prefs = BPreferences
                .set("PREF3", "value3")
                .set("PREF1", "value1")
                .set("PREF2", "value2")
                .assemble();

        String expected = "[PREF1=value1, PREF2=value2, PREF3=value3]";
        String actual = prefs.toString();

        assertEquals(expected, actual);
    }

}
