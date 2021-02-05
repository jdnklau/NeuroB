package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
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
        BPreferences prefs1 = BPreferences.set("FOO", "BAR").assemble();
        BPreferences prefs2 = BPreferences.set("FOO", "BUZ").assemble();
        BPreferences prefs3 = BPreferences.set("FIZ", "BUZ").assemble();
        int actual = Labels.parseLabellingSize("smult", new Backend[]{new ProBBackend(), new ProBBackend(prefs1), new ProBBackend(prefs2), new ProBBackend(prefs3)});

        assertEquals(4, actual);
    }

}
