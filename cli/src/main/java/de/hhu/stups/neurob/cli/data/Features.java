package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.cli.formats.Formats;
import de.hhu.stups.neurob.core.features.FeatureGenerating;
import de.hhu.stups.neurob.core.features.predicates.BAst109Reduced;
import de.hhu.stups.neurob.core.features.predicates.BAst110Features;
import de.hhu.stups.neurob.core.features.predicates.BAst115Features;
import de.hhu.stups.neurob.core.features.predicates.BAst124Features;
import de.hhu.stups.neurob.core.features.predicates.BAst185Features;
import de.hhu.stups.neurob.core.features.predicates.BAst275Features;
import de.hhu.stups.neurob.core.features.predicates.GenericNormalisedPredicate;
import de.hhu.stups.neurob.core.features.predicates.NormalisedPredicate;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.predicates.RawPredFeature;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public enum Features {

    F109("109 features over the B Ast", () -> new BAst109Reduced.Generator(), 109),
    F110("110 features over the B Ast", () -> new BAst110Features.Generator(), 110),
    F115("115 features over the B Ast", () -> new BAst115Features.Generator(), 115),
    F124("124 features over the B Ast", () -> new BAst124Features.Generator(), 124),
    F185("185 features over the B Ast", () -> new BAst185Features.Generator(), 185),
    F275("275 features over the B Ast", () -> new BAst275Features.Generator(), 275),

    RAW("Raw predicates as features; not usable with CSV format",
            () -> new RawPredFeature.Generator(), -1),
    NRM("Normalised predicates as features (x+y -> id1+id2); not usable with CSV format",
            () -> new NormalisedPredicate.Generator(), -1),
    GNRM("Generic normalised predicates as features (x+y -> id+id); not usable with CSV format",
            () -> new GenericNormalisedPredicate.Generator(), -1),

    ;

    public final String info;
    public final Callable<PredicateFeatureGenerating> generator;
    public final int vecSize;

    Features(String info, Callable<PredicateFeatureGenerating> gen, int vectorSize) {
        this.info = info;
        this.generator = gen;
        this.vecSize = vectorSize;
    }

    public static String getFeatureInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Supported features:\n");

        for (Features f : Features.values()) {
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

    public static PredicateFeatureGenerating parseFormat(String id) throws Exception {
        return Features.valueOf(id.toUpperCase()).generator.call();
    }

    public static Integer parseFeatureSize(String id) throws Exception {
        return Features.valueOf(id.toUpperCase()).vecSize;
    }
}
