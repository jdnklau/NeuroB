package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;

public interface TrainingDbFormat<D, L extends Labelling>
    extends TrainingDataFormat<D, L> {
}
