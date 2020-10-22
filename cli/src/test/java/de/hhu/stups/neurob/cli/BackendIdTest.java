package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BackendIdTest {

    @Test
    void shouldMatchProB() {
        String line = "prob[...]";
        BackendId expected = BackendId.PROB;
        BackendId actual = BackendId.matchBackend(line);
        assertEquals(expected, actual);
    }

    @Test
    void shouldMatchKodkod() {
        String line = "kodkod[...]";
        BackendId expected = BackendId.KODKOD;
        BackendId actual = BackendId.matchBackend(line);
        assertEquals(expected, actual);
    }

    @Test
    void shouldMatchZ3() {
        String line = "z3[...]";
        BackendId expected = BackendId.Z3;
        BackendId actual = BackendId.matchBackend(line);
        assertEquals(expected, actual);
    }

    @Test
    void shouldMatchSmt() {
        String line = "smt[...]";
        BackendId expected = BackendId.SMT;
        BackendId actual = BackendId.matchBackend(line);
        assertEquals(expected, actual);
    }

    @Test
    void shouldParsePreferences() {
        String prefs = "FOO=BAR,FIZ=BAZ";

        BPreference[] expected = {
                new BPreference("FOO", "BAR"),
                new BPreference("FIZ", "BAZ")
        };
        BPreference[] actual = BackendId.parsePrefs(prefs);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldCrossProducePreferences() {
        BPreference foo = new BPreference("FOO", "BAR");
        BPreference fiz = new BPreference("FIZ", "BAZ");
        BPreference[] prefs = {foo, fiz};

        Set<Set<BPreference>> expected = new HashSet<>();
        Set<BPreference> toAdd = new HashSet<>();
        // none
        expected.add(toAdd);
        // foo
        toAdd = new HashSet<>();
        toAdd.add(foo);
        expected.add(toAdd);
        // fiz
        toAdd = new HashSet<>();
        toAdd.add(fiz);
        expected.add(toAdd);
        // foo+fiz
        toAdd = new HashSet<>();
        toAdd.add(foo);
        toAdd.add(fiz);
        expected.add(toAdd);

        Set<Set<BPreference>> actual = BackendId.crossProducePrefsNoTimeouts(prefs).collect(Collectors.toSet());

        assertEquals(expected, actual);
    }

}
