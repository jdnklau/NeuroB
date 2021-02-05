package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.cli.formats.Formats;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.labelling.LabelGenerating;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;

import java.util.Arrays;

public class MigrationFormat {
    private final PredicateFeatureGenerating features;
    private final LabelTranslation labels;
    private final TrainingDataFormat format;

    public MigrationFormat(String featureId, String labelId, String formatId, Backend[] backends) throws Exception {

        this.features = Features.parseFormat(featureId);
        this.labels = Labels.parseLabelling(labelId, backends);
        this.format = Formats.parseFormat(
                formatId,
                Features.parseFeatureSize(featureId),
                Labels.parseLabellingSize(labelId, backends),
                Arrays.asList(backends.clone()));
    }

    public PredicateFeatureGenerating getFeatures() {
        return features;
    }

    public LabelTranslation getLabels() {
        return labels;
    }

    public TrainingDataFormat getFormat() {
        return format;
    }
}
