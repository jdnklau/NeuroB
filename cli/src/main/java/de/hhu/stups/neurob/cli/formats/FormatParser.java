package de.hhu.stups.neurob.cli.formats;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface FormatParser {
    TrainingDataFormat get(int features, int labels, List<Backend> backends);

    default TrainingDataFormat get(int features, int labels) {
        return get(features, labels, new ArrayList<>());
    }
}
