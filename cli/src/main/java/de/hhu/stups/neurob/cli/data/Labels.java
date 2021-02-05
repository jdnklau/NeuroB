package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.core.labelling.SettingsMultiLabel;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;

import java.util.concurrent.Callable;
import java.util.function.Function;

public enum Labels {
    BC("Classification over the given Backends", b -> new BackendClassification.Translator(b), b->1),
    SMULT("Multi-Labelling of Settings", b -> new SettingsMultiLabel.Translator(b), b->(b.length))
    ;

    public final String info;
    public final Function<Backend[], LabelTranslation> generatorSetup;
    final public Function<Backend[], Integer> labelSize;

    Labels(String info,
            Function<Backend[], LabelTranslation> generator,
            Function<Backend[], Integer> calcLabelSize) {
        this.info = info;
        this.generatorSetup = generator;
        this.labelSize = calcLabelSize;
    }

    public static String getLabelInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Supported labellings:\n");

        for (Labels f : Labels.values()) {
            info.append(" - ");
            int length = 27 - f.name().length();
            while (length < 0) {
                length += 4;
            }
            info.append(f.name().toLowerCase());
            for (int i = 0; i < length; i++) {
                info.append(' ');
            }
            info.append(f.info);
            info.append('\n');
        }

        return info.toString();

    }

    public Integer getLabellingSize(Backend[] backends) {
        return labelSize.apply(backends);
    }

    public static Integer parseLabellingSize(String lbl, Backend[] backends){
        return Labels.valueOf(lbl.toUpperCase()).getLabellingSize(backends);
    }

    public static LabelTranslation parseLabelling(String lbl, Backend[] backends) {
        return Labels.valueOf(lbl.toUpperCase()).generatorSetup.apply(backends);
    }
}
