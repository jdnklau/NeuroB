package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;

import java.io.IOException;
import java.nio.file.Path;

public class PredDbFiltering {

    void filter(Path from, PredicateDbFormat<PredDbEntry> fmt, Path to, Backend[] backends) throws IOException {
        fmt.loadTrainingData(from)
                .map(this::strictlyFilterSamples)
                .forEach(samps -> {
                    try {
                        fmt.writeSamples(samps, to);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    TrainingData<BPredicate, PredDbEntry> strictlyFilterSamples(TrainingData<BPredicate, PredDbEntry> data) {
        TrainingData<BPredicate, PredDbEntry> newData;

        var results = data.getSamples()
                .filter(this::isCompleteAndSound);

        newData = new TrainingData<>(data.getSourceFile(), results);

        return newData;
    }

    /**
     * Checks whether the given sample has no contradictions stored and all backends have an error-free
     * result.
     *
     * @param sample
     * @return
     */
    private boolean isCompleteAndSound(TrainingSample<BPredicate, PredDbEntry> sample) {
        boolean hasValidAnswer = false;
        boolean hasInvalidAnswer = false;

        for (Backend b : sample.getLabelling().getBackendsUsed()) {
            var answer = sample.getLabelling().getResult(b);

            if (null == answer) {
                return false;
            }

            switch (answer.getAnswer()) {
                case VALID:
                    hasValidAnswer = true;
                    break;
                case INVALID:
                    hasInvalidAnswer = true;
                    break;
                case ERROR:
                    return false;
                default:
                    break;
            }
        }

        return !hasInvalidAnswer || !hasValidAnswer;
    }

}
