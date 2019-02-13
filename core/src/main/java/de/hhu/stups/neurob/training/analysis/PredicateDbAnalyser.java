package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;

import java.io.IOException;
import java.nio.file.Path;

public class PredicateDbAnalyser {

    /** Format of the database(s) to be analysed. */
    private final PredicateDbFormat<PredDbEntry> format;

    public PredicateDbAnalyser(PredicateDbFormat<PredDbEntry> format) {
        this.format = format;
    }

    public PredDbAnalysis analyse(Path db) throws IOException {
        PredDbAnalysis data = new PredDbAnalysis();
        format.loadTrainingData(db)
                .flatMap(TrainingData::getSamples)
                .forEach(data::add); // TODO: Check whether close is called correctly.

        return data;
    }
}
