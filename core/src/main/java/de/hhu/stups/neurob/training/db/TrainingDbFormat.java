package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public interface TrainingDbFormat<D, L extends Labelling>
        extends TrainingDataFormat<D, L> {

    default Stream<TrainingData<D, L>> loadTrainingData(Path source) throws IOException {
        return Files.walk(source)
                .filter(p -> p.toString().endsWith(getFileExtension())) // only account for matching files
                .map(dbFile ->
                {
                    try {
                        return new TrainingData<>(
                                this.getDataSource(dbFile),
                                this.loadSamples(dbFile));
                    } catch (IOException e) {
                        // TODO: handle properly
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    /**
     * Retrieves the original path of the B machine from which the dbFile was created.
     *
     * The promise is, that if the dbFile at least references one file as original source,
     * the first such file is returned. Might be null if no such file exists.
     *
     * @param dbFile Path to data base file created by this format.
     *
     * @return First referenced source machine in dbFile or null
     */
    Path getDataSource(Path dbFile) throws IOException;
}
