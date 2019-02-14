package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class PredicateDbAnalyser {

    private static final Logger log = LoggerFactory.getLogger(PredicateDbAnalyser.class);

    /** Format of the database(s) to be analysed. */
    private final PredicateDbFormat<PredDbEntry> format;

    public PredicateDbAnalyser(PredicateDbFormat<PredDbEntry> format) {
        this.format = format;
    }

    public PredDbAnalysis analyse(Path db) throws IOException {
        log.info("Analysing Database located at {}", db);

        PredDbAnalysis analysis = new PredDbAnalysis();
        format.loadTrainingData(db)
                .parallel()
                .map(this::analyse)
                .forEach(analysis::mergeWith); // TODO: Check whether close is called correctly.

        return analysis;
    }

    public PredDbAnalysis analyse(TrainingData<BPredicate, PredDbEntry> data) {
        PredDbAnalysis analysis = new PredDbAnalysis();
        data.getSamples().forEach(analysis::add);
        return analysis;
    }
}
