package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.core.labelling.LabelGenerating;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelsTest {

    @Test
    void shouldGetBackendSelection() {
        LabelTranslation labelling = Labels.parseLabelling("bc", new Backend[]{new ProBBackend(), new KodkodBackend()});

        assertTrue (labelling instanceof BackendClassification.Translator);
    }

    @Test
    void shouldGetBackendSelectionLabellingSize() {
        int actual = Labels.parseLabellingSize("bc", new Backend[]{new ProBBackend(), new KodkodBackend()});

        assertEquals(1, actual);
    }

    @Test
    void shouldGetSettingSelectionLabellingSize() {
        BPreference pref1 = BPreference.set("SMT", "TRUE");
        BPreference pref2 = BPreference.set("CHR", "TRUE");
        BPreference pref3 = BPreference.set("CSE", "TRUE");
        BPreference pref4 = BPreference.set("SOLVER_STRENGTH", "50");
        BPreference pref5 = BPreference.set("SOLVER_STRENGTH", "100");
        int actual = Labels.parseLabellingSize("smult", new Backend[]{
                new ProBBackend(),
                new ProBBackend(pref1),
                new ProBBackend(pref2),
                new ProBBackend(pref3),
                new ProBBackend(pref4),
                new ProBBackend(pref5),
                new ProBBackend(pref1, pref2),
                new ProBBackend(pref1, pref3),
                new ProBBackend(pref1, pref4),
                new ProBBackend(pref1, pref5),
                new ProBBackend(pref2, pref3),
                new ProBBackend(pref2, pref4),
                new ProBBackend(pref2, pref5),
                new ProBBackend(pref3, pref4),
                new ProBBackend(pref3, pref5),
                new ProBBackend(pref4, pref5),
                new ProBBackend(pref1, pref2, pref3),
                new ProBBackend(pref1, pref2, pref4),
                new ProBBackend(pref1, pref2, pref5),
                new ProBBackend(pref1, pref3, pref4),
                new ProBBackend(pref1, pref3, pref5),
                new ProBBackend(pref2, pref3, pref4),
                new ProBBackend(pref2, pref3, pref5),
                new ProBBackend(pref2, pref4, pref5),
                new ProBBackend(pref1, pref2, pref3, pref4),
                new ProBBackend(pref1, pref2, pref3, pref5),
        });

        assertEquals(5, actual);
    }

}
