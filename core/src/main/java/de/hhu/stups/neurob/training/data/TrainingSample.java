package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Pair of data point and corresponding labelling.
 * @param <D> Data type that is labelled
 * @param <L>
 */
public class TrainingSample<D, L extends Labelling> {

    private final D data;
    private final L labelling;
    private final Path sourceFile;
    private final String comment;

    public TrainingSample(D features, L labelling) {
        this(features, labelling, null);
    }

    public TrainingSample(D features, L labelling, Path sourceFile) {
        this(features, labelling, sourceFile, null);
    }

    public TrainingSample(D features, L labelling, Path sourceFile, String comment) {
        this.data = features;
        this.labelling = labelling;
        this.sourceFile = sourceFile;
        this.comment = comment;
    }

    /**
     * Returns the encapsulated data.
     *
     * @return
     * @deprecated Use {@link #getData()} instead.
     */
    @Deprecated
    public D getFeatures() {
        return data;
    }

    /**
     * Returns the encapsulated data.
     *
     * @return
     */
    public D getData() {
        return data;
    }

    /**
     * Returns the labelling associated with the encapsulated data.
     * @return
     */
    public L getLabelling() {
        return labelling;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        String sourceMention = (sourceFile == null)
                ? ""
                : ", source=" + sourceFile;
        return "[data=" + data.toString()
               + ", labels="
               + labelling.getLabellingString()
               + sourceMention
               + "]";
    }

    @Override
    public boolean equals(Object sample) {
        if (sample == null) {
            return false;
        }

        if (sample.getClass().equals(this.getClass())) {
            TrainingSample trainingSample = (TrainingSample) sample;

            // Check for each value pair whether exactly one value is null
            if (data == null ^ trainingSample.data == null) {
                return false;
            }
            if (labelling == null ^ trainingSample.labelling == null) {
                return false;
            }
            if (sourceFile == null ^ trainingSample.sourceFile == null) {
                return false;
            }

            // Due to check above, if data are nonnull
            // then trainingSample.data are also nonnull
            if (data != null && labelling != null) {
                return data.equals(trainingSample.data)
                       && this.labelling.equals(trainingSample.labelling)
                       && ((sourceFile == null)
                           || sourceFile.equals(trainingSample.sourceFile));
            } else {
                return sourceFile == null
                       || sourceFile.equals(trainingSample.sourceFile);
            }
        }

        return false;
    }

}
