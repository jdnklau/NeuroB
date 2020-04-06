package de.hhu.stups.neurob.cli.formats;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface FormatParser {
    TrainingDataFormat get(List<Backend> backends);

    default TrainingDataFormat get() {
        return get(new ArrayList<>());
    }
}
