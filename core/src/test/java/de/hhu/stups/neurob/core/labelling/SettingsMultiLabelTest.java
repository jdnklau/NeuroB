package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SettingsMultiLabelTest {

    private final BPreference foo0 = new BPreference("FOO", "0");
    private final BPreference foo1 = new BPreference("FOO", "1");
    private final BPreference foo2 = new BPreference("FOO", "2");
    private final BPreference bar0 = new BPreference("BAR", "0");
    private final BPreference baz0 = new BPreference("BAZ", "0");
    private final BPreference to = new BPreference("TIME_OUT", "10000");

    @Test
    void shouldCrossSettings() {
        BPreference[] prefs = {foo0, bar0, baz0};

        Set<Backend> expected = new HashSet<>();
        expected.add(new ProBBackend(foo0, bar0, baz0));
        expected.add(new ProBBackend(foo0, bar0));
        expected.add(new ProBBackend(foo0, baz0));
        expected.add(new ProBBackend(bar0, baz0));
        expected.add(new ProBBackend(foo0));
        expected.add(new ProBBackend(bar0));
        expected.add(new ProBBackend(baz0));
        expected.add(new ProBBackend());

        Set<Backend> actual = Arrays
                .stream(SettingsMultiLabel.Translator.assembleBackends(2500L, prefs))
                .collect(Collectors.toSet());

        assertEquals(expected, actual, "Generated backends do not match");
    }

    @Test
    void shouldUseAllVariantsOfCategoricalSettings() {
        BPreference[] prefs = {foo0, bar0, foo1, foo2};

        Set<Backend> expected = new HashSet<>();
        expected.add(new ProBBackend(foo0, bar0));
        expected.add(new ProBBackend(foo1, bar0));
        expected.add(new ProBBackend(foo2, bar0));
        expected.add(new ProBBackend(foo0));
        expected.add(new ProBBackend(foo1));
        expected.add(new ProBBackend(foo2));
        expected.add(new ProBBackend(foo2));
        expected.add(new ProBBackend(bar0));
        expected.add(new ProBBackend());

        Set<Backend> actual = Arrays
                .stream(SettingsMultiLabel.Translator.assembleBackends(2500L, prefs))
                .collect(Collectors.toSet());

        assertEquals(expected, actual, "Generated backends do not match");
    }

    @Test
    void shouldMatchArrayPositionOfPreference() {
        BPreference[] prefs = {foo0, bar0, baz0};
        BPreferences used = new BPreferences(baz0, foo0);

        Double[] expected = {1., 0., 1.};
        Double[] actual = SettingsMultiLabel.Translator.genSettingsArray(used, prefs);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldMatchArrayPositionForCategoricalPreferences() {
        BPreference[] prefs = {foo0, foo1, foo2, bar0, baz0};
        BPreferences used = new BPreferences(baz0, foo2);

        Double[] expected = {0., 0., 1., 0., 1.};
        Double[] actual = SettingsMultiLabel.Translator.genSettingsArray(used, prefs);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldIgnoreTimeouts() {
        BPreference[] prefs = {foo0, to};

        Set<Backend> expected = new HashSet<>();
        expected.add(new ProBBackend(foo0));
        expected.add(new ProBBackend());

        Set<Backend> actual = Arrays
                .stream(SettingsMultiLabel.Translator.assembleBackends(2500L, prefs))
                .collect(Collectors.toSet());

        assertEquals(expected, actual, "Generated backends do not match");
    }
}
