package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.data.BData;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface TrainingDbFormat<D extends BData, L extends Labelling>
    extends TrainingDataFormat<D, L> {
}
