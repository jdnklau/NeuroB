package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BElement;
import de.hhu.stups.neurob.core.features.Features;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface TrainingDbFormat<F extends Features, B extends BElement>
        extends TrainingDataFormat<F> {

    Stream<DbSample<B>> loadSamples(Path sourceFile) throws IOException;
}
