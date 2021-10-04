package de.hhu.stups.neurob.core.api.backends.preferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Preference collection containing preferences for B Backends.
 * <p>
 * Easiest way for assembling a preference collection would be to use
 * the builder pattern:
 * <pre>
 * BPreferences
 *     .set("PREF1", "value1")
 *     .set("PREF2", "value2")
 *     .set("PREF3", "value3")
 *     .assemble();
 * </pre>
 */
public class BPreferences {

    private final SortedMap<String, BPreference> preferences;

    /**
     * Constructs a preference collection from the given preferences.
     * If the same preference is assigned multiple values, the last value is taken.
     *
     * @param prefs
     */
    public BPreferences(BPreference... prefs) {
        SortedMap<String, BPreference> prefMap = new TreeMap<>();
        Arrays.stream(prefs).forEach(p -> prefMap.put(p.getName(), p));

        this.preferences = Collections.unmodifiableSortedMap(prefMap);
    }

    public BPreferences(SortedMap<String, BPreference> preferenceMap) {
        // Create an immutable copy from the given preference map
        this.preferences = Collections.unmodifiableSortedMap(new TreeMap<>(preferenceMap));
    }

    /**
     * Constructs a preference collection from the given map
     * from
     *
     * @param preferenceMap
     */
    public BPreferences(Map<String, String> preferenceMap) {
        SortedMap<String, BPreference> backendPrefMap = new TreeMap<>();
        preferenceMap.keySet().stream()
                .map(prefName -> BPreference.set(prefName, preferenceMap.get(prefName)))
                .forEach(pref -> backendPrefMap.put(pref.getName(), pref));
        // Create an immutable copy from the given preference map
        this.preferences = Collections.unmodifiableSortedMap(new TreeMap<>(backendPrefMap));
    }

    public boolean contains(String name) {
        return preferences.containsKey(name);
    }

    public boolean contains(BPreference preference) {
        return preferences.containsValue(preference);
    }

    public BPreference get(String name) {
        return preferences.get(name);
    }

    public BPreference getOrDefault(String name, BPreference def) {
        return preferences.getOrDefault(name, def);
    }

    public Stream<BPreference> stream() {
        return preferences.keySet().stream().map(preferences::get);
    }

    public BPreferences without(String... blacklistedPreference) {
        if (blacklistedPreference.length == 0)
            return this;

        SortedMap<String, BPreference> reducedPreferences = new TreeMap<>();

        preferences.forEach((key, value) -> {
            boolean keep = true;
            for (String blocked : blacklistedPreference) {
                if (key.equals(blocked)) {
                    keep = false;
                }
            }
            if (keep) {
                reducedPreferences.put(key, value);
            }
        });

        return new BPreferences(reducedPreferences);
    }

    @Override
    public String toString() {
        String preferencesString = stream()
                // Settings which are false are equal to being turned off.
                .filter(p -> !p.getValue().equals("FALSE"))
                .map(BPreference::toString)
                .collect(Collectors.joining(", "));
        return "[" + preferencesString + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BPreferences) {
            BPreferences other = (BPreferences) o;
            return preferences.equals(other.preferences);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return preferences.hashCode();
    }

    /**
     * Accesses the Builder pattern.
     * Allows to chain {@link #set(String, String)} methods,
     * followed by an {@link Builder#assemble assemble()} call to
     * build the preferences.
     *
     * @param name Name of the preference
     * @param value Value to be set
     *
     * @return BPreference builder for method chaining
     */
    public static Builder set(String name, String value) {
        return new Builder(name, value);
    }

    public static class Builder {
        Map<String, String> preferences;

        public Builder(String name, String value) {
            preferences = new HashMap<>();
            preferences.put(name, value);
        }

        public Builder set(String name, String value) {
            preferences.put(name, value);
            return this;
        }

        public BPreferences assemble() {
            return new BPreferences(preferences);
        }
    }
}
