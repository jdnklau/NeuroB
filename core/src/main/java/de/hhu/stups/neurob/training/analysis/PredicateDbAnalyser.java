package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class PredicateDbAnalyser {

    private static final Logger log = LoggerFactory.getLogger(PredicateDbAnalyser.class);

    /** Format of the database(s) to be analysed. */
    private final PredicateDbFormat<PredDbEntry> format;

    public PredicateDbAnalyser(PredicateDbFormat<PredDbEntry> format) {
        this.format = format;
    }

    public PredDbAnalysis analyse(Path db) throws IOException {
        log.info("Analysing Database located at {}", db);

        return format.loadTrainingData(db)
                .parallel()
                .map(this::analyse)
                .collect(new AnalysisCollector());
    }

    public PredDbAnalysis analyse(TrainingData<BPredicate, PredDbEntry> data) {
        PredDbAnalysis analysis = new PredDbAnalysis();
        data.getSamples().forEach(analysis::add);
        return analysis;
    }

    public static class AnalysisCollector implements Collector<PredDbAnalysis,PredDbAnalysis,PredDbAnalysis> {

        @Override
        public Supplier<PredDbAnalysis> supplier() {
            return PredDbAnalysis::new;
        }

        @Override
        public BiConsumer<PredDbAnalysis, PredDbAnalysis> accumulator() {
            return PredDbAnalysis::mergeWith;
        }

        @Override
        public BinaryOperator<PredDbAnalysis> combiner() {
            return PredDbAnalysis::mergeWith;
        }

        @Override
        public Function<PredDbAnalysis, PredDbAnalysis> finisher() {
            return a->a;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.CONCURRENT, Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);
        }
    }
}
