package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import lombok.experimental.var;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public enum BackendId {

    PROB("prob", ProBBackend::new),
    KODKOD("kodkod", KodkodBackend::new),
    Z3("z3", Z3Backend::new),
    SMT("smt", SmtBackend::new);


    private final String id;
    private final Function<BPreferences, Backend> generator;

    BackendId(String id, Function<BPreferences, Backend> generator) {
        this.id = id;
        this.generator = generator;
    }

    public static String backendInfo =
            "Supported backends are:\n"
            + " - prob:                      Native Prolog backend\n"
            + " - kodkod:                    The Kodkod binding\n"
            + " - z3:                        The Z3 binding\n"
            + " - smt:                       The SMT_SUPPORTED_INTERPRETERS option\n"
            + "\n"
            + "For each Backend, an optional list of options can be given, e.g.:\n"
            + "   -b prob[TIME_OUT=3000,MAXINT=17] z3[TIME_OUT=1200]\n"
            + "\n"
            + "If the option -x is set, any combination of options is tested.\n"
            + "The -x option will not consider the timeout setting if only one\n"
            + "is provided:\n"
            + "   -xb prob[TIME_OUT=3000,MAXINT=17]\n"
            + "for instance would use the backend configurations\n"
            + "   - prob[TIME_OUT=3000], and\n"
            + "   - prob[TIME_OUT=3000,MAXINT=17]\n"
            + "but not the configurations\n"
            + "   - prob[], and\n"
            + "   - prob[MAXINT=17].\n";

    public static Backend[] makeBackends(String parameter, boolean allPrefCombinations) {
        BackendId b = matchBackend(parameter);

        // check for preferences
        BPreference[] prefs = {};
        if (b.id.length() < parameter.length()) {
            int start = b.id.length() + 1;
            int end = parameter.length() - 1;
            String prefString = parameter.substring(start, end);

            prefs = parsePrefs(prefString);

        }

        if (!allPrefCombinations) {
            return new Backend[]{b.generator.apply(new BPreferences(prefs))};
        }

        return crossProducePrefs(prefs)
                .map(ps -> ps.toArray(new BPreference[0]))
                .map(BPreferences::new)
                .distinct()
                .map(b.generator)
                .toArray(Backend[]::new);

    }

    static BackendId matchBackend(String parameter) {
        for (BackendId b : BackendId.values()) {
            if (parameter.startsWith(b.id)) {
                return b;
            }
        }

        System.out.println("Unknown backend setting " + parameter);
        return null;
    }

    static BPreference[] parsePrefs(String prefString) {
        String[] prefArray = prefString.split(",");
        return Arrays.stream(prefArray)
                .map(pref -> {
                    String[] entry = pref.split("=");
                    return new BPreference(entry[0], entry[1]);
                })
                .toArray(BPreference[]::new);
    }

    static Stream<Set<BPreference>> crossProducePrefs(BPreference[] prefs) {
        // find timeout
        List<BPreference> crossPrefs = new ArrayList<>();
        Set<BPreference> timeouts = new HashSet<>();
        for (int i = 0; i < prefs.length; i++) {
            BPreference pref = prefs[i];
            if (pref.getName().equals("TIME_OUT")
                || pref.getName().equals("TIMEOUT")) {
                timeouts.add(pref);
            } else {
                crossPrefs.add(pref);
            }
        }

        BPreference[] prefArr =
                crossPrefs.toArray(new BPreference[crossPrefs.size()]);
        Stream<Set<BPreference>> result;

        if (timeouts.isEmpty()) {
            result = crossProducePrefsNoTimeouts(prefArr);
        } else {
            result = timeouts.stream()
                    .flatMap(to ->
                            crossProducePrefsNoTimeouts(prefArr).map(
                                    l -> {
                                        l.add(to);
                                        return l;
                                    }
                            ));
        }

        return result;
    }

    static Stream<Set<BPreference>> crossProducePrefsNoTimeouts(BPreference[] prefs) {
        int length = prefs.length;

        if (length <= 0) {
            return Stream.of(new HashSet<>());
        }

        BPreference head = prefs[0];
        BPreference[] tail = new BPreference[length - 1];
        System.arraycopy(prefs, 1, tail, 0, length - 1);

        return Stream.of(head, null)
                .flatMap(p -> p != null
                        ? crossProducePrefsNoTimeouts(tail)
                        .map(l -> {
                            l.add(p);
                            return l;
                        })
                        : crossProducePrefsNoTimeouts(tail));
    }
}
