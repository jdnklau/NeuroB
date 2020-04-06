package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;

public interface PredicateDbFormat<L extends PredicateLabelling>
        extends TrainingDbFormat<BPredicate, L> {
}
