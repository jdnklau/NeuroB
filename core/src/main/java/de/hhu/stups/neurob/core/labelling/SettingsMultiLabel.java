package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SettingsMultiLabel extends PredicateLabelling {
    protected final BPreference[] preferences;
    protected final int prefCount;

    public SettingsMultiLabel(String predicate, BPreference[] prefs,
            Double[] labellingArray) {
        this(BPredicate.of(predicate), prefs, labellingArray);
    }

    public SettingsMultiLabel(BPredicate predicate, BPreference[] prefs,
            Double[] labellingArray) {
        super(predicate, labellingArray);
        this.preferences = prefs;
        this.prefCount = prefs.length;
    }

    public static class Generator implements PredicateLabelGenerating<SettingsMultiLabel> {
        private final BPreference[] prefs;
        private final Backend[] backends;
        private final long timeoutMS;

        public Generator(Long timeoutMS, BPreference[] preferences) {
            this.prefs = preferences;
            this.backends = Translator.assembleBackends(timeoutMS, preferences);
            this.timeoutMS = timeoutMS;
        }

        @Override
        public SettingsMultiLabel generate(BPredicate predicate, MachineAccess bMachine) throws LabelCreationException {
            // FIXME: the core package should not have any dependencies to the training package
            // Generate predicate data.
            PredDbEntry dbEntry = new PredDbEntry.Generator(1, backends).generate(predicate, bMachine);

            return new Translator(prefs, backends).translate(dbEntry);
        }


    }

    public static class Translator implements LabelTranslation<PredDbEntry, SettingsMultiLabel> {

        private final BPreference[] prefs;
        private final Backend[] backends;

        public Translator(long timeoutMS, BPreference[] preferences) {
            this.prefs = preferences;
            this.backends = assembleBackends(timeoutMS, preferences);
        }

        Translator(BPreference[] preferences, Backend[] backends) {
            this.prefs = preferences;
            this.backends = backends;
        }

        @Override
        public SettingsMultiLabel translate(PredDbEntry dbEntry) {
            Backend fastest = BackendClassification.classifyFastestBackend(backends, dbEntry.getResults());
            Double[] labels = genSettingsArray(fastest.getPreferences(), prefs);
            return new SettingsMultiLabel(dbEntry.getPredicate(), prefs, labels);
        }

        static Backend[] assembleBackends(Long timeoutMS, BPreference[] preferences) {
            Stack<BPreference> prefStack = new Stack<>();
            for (BPreference p : preferences) {
                prefStack.push(p);
            }
            Stack<Backend> init = new Stack<>();
            init.push(new ProBBackend(timeoutMS, TimeUnit.MILLISECONDS));
            Stack<Backend> backends = crossProduceBackends(
                    init, prefStack
            );
            return backends.toArray(new Backend[0]);
        }

        static Double[] genSettingsArray(BPreferences used, BPreference[] orderedPrefs) {
            int num = orderedPrefs.length;
            Double[] labels = new Double[num];
            for (int i = 0; i < num; i++) {
                BPreference currentPref = orderedPrefs[i];
                boolean isPrefSet = used.contains(currentPref);
                labels[i] = (isPrefSet)? 1. : 0.;
            }
            return labels;
        }

        static Stack<Backend> crossProduceBackends(
                Stack<Backend> currentStack, Stack<BPreference> prefStack) {
            if (prefStack.empty()) {
                return currentStack;
            }

            BPreference pref = prefStack.pop();
            // Ignore Timeouts
            if (pref.getName().equals("TIME_OUT")) {
                return crossProduceBackends(currentStack, prefStack);
            }

            Stack<Backend> newStack = new Stack<>();
            // For each backend, add another with the same set of preferences and an additional one.
            for (Backend b : currentStack) {
                BPreferences origPrefs = b.getPreferences();
                List<BPreference> prefList = origPrefs.stream().collect(Collectors.toList());
                prefList.add(pref);
                BPreferences newPrefs = new BPreferences(prefList.toArray(new BPreference[0]));
                newStack.push(b);
                newStack.push(new ProBBackend(newPrefs));
            }

            return crossProduceBackends(newStack, prefStack);
        }
    }
}
