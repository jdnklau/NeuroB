package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DataCliTest {

    @Test
    void shouldParseSingleBackend() {
        String[] backendIds = {"prob"};

        List<Backend> expected = backendList(new ProBBackend());
        List<Backend> actual = new DataCli().parseBackends(backendIds, false);

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseSingleBackendWhenCrossCreatingWithoutParameters() {
        String[] backendIds = {"prob"};

        List<Backend> expected = backendList(new ProBBackend());
        List<Backend> actual = new DataCli().parseBackends(backendIds, true);

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseBackendsWhenCrossCreatingSingleBackend() {
        String[] backendIds = {"prob[FOO=BAR,FIZ=BAZ]"};

        BPreference foo = BPreference.set("FOO", "BAR");
        BPreference fiz = BPreference.set("FIZ", "BAZ");

        Set<Backend> expected = new HashSet<>(backendList(
                new ProBBackend(),
                new ProBBackend(foo),
                new ProBBackend(fiz),
                new ProBBackend(foo, fiz)
        ));
        Set<Backend> actual =
                new HashSet<>(new DataCli().parseBackends(backendIds, true));

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseBackendsWhenCrossCreatingMultipleBackends() {
        String[] backendIds = {"prob[FOO=BAR,FIZ=BAZ]",
                               "z3[BRRAP=BRRAPP"};

        BPreference foo = BPreference.set("FOO", "BAR");
        BPreference fiz = BPreference.set("FIZ", "BAZ");
        BPreference brrap = BPreference.set("BRRAP", "BRRAP");

        Set<Backend> expected = new HashSet<>(backendList(
                new ProBBackend(),
                new ProBBackend(foo),
                new ProBBackend(fiz),
                new ProBBackend(foo, fiz),
                new Z3Backend(),
                new Z3Backend(brrap)
        ));
        Set<Backend> actual =
                new HashSet<>(new DataCli().parseBackends(backendIds, true));

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseBackendsWhenNotCrossCreatingMultipleBackends() {
        String[] backendIds = {"prob[FOO=BAR,FIZ=BAZ]",
                "z3[BRRAP=BRRAPP"};

        BPreference foo = BPreference.set("FOO", "BAR");
        BPreference fiz = BPreference.set("FIZ", "BAZ");
        BPreference brrap = BPreference.set("BRRAP", "BRRAP");

        Set<Backend> expected = new HashSet<>(backendList(
                new ProBBackend(foo, fiz),
                new Z3Backend(brrap)
        ));
        Set<Backend> actual =
                new HashSet<>(new DataCli().parseBackends(backendIds, false));

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseSourceDirectory() throws ParseException {
        String cliInput = "-g foo/ -t bar/ jsondb";
        CommandLine line = parseCommandLine(cliInput);

        Path expected = Paths.get("foo");
        Path actual = new DataCli().parseSourceDirectory(line, "g");

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseCores() throws ParseException {
        String cliInput = "-g foo/ -t bar/ jsondb -c 6";
        CommandLine line = parseCommandLine(cliInput);

        int expected = 6;
        int actual = new DataCli().parseCores(line);

        assertEquals(expected, actual);
    }

    @Test
    void shouldUseDefaultCoreValue() throws ParseException {
        String cliInput = "-g foo/ -t bar/ jsondb";
        CommandLine line = parseCommandLine(cliInput);

        int expected = Runtime.getRuntime().availableProcessors() - 1; // Java default for ForkPool
        int actual = new DataCli().parseCores(line);

        assertEquals(expected, actual);
    }

    @Test
    void shouldParseTargetDirectory() throws ParseException {
        String cliInput = "-g foo/ -t bar/ jsondb";
        CommandLine line = parseCommandLine(cliInput);

        Path expected = Paths.get("bar");
        Path actual = new DataCli().parseTargetDirectory(line);

        assertEquals(expected, actual);
    }

    private List<Backend> backendList(Backend... backends) {
        return Arrays.stream(backends).collect(Collectors.toList());
    }

    private CommandLine parseCommandLine(String cliInput) throws ParseException {
        String[] args = cliInput.split(" ");
        return new DefaultParser().parse(new DataCli().options, args);
    }

}
