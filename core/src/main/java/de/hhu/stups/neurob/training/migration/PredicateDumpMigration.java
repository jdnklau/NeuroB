package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.backends.KodKodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.TrainingDbFormat;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class PredicateDumpMigration {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDumpMigration.class);

    public void migrate(Path source, Path target, TrainingDbFormat format) {
        // TODO implement
    }

    /**
     * Translate a given Predicate Dump into a DbSample.
     *
     * @param pdump
     *
     * @return
     */
    public DbSample<BPredicate> translate(PredicateDump pdump) {
        BPredicate predicate = pdump.getPredicate();
        Labelling labels = new Labelling(
                pdump.getTime(ProBBackend.class),
                pdump.getTime(KodKodBackend.class),
                pdump.getTime(Z3Backend.class),
                pdump.getTime(SmtBackend.class)
        );
        Path source = pdump.getSource();

        return new DbSample<>(predicate, labels, source);
    }

    /**
     * Streams the translated samples from the given *.pdump file.
     *
     * @param source A predicate dump file
     *
     * @return Stream of DbSamples
     */
    public Stream<DbSample<BPredicate>> streamTranslatedSamples(Path source) throws IOException {
        Stream<String> entries = Files.lines(source);

        // #source annotations define the source machine for all following
        // lines; value is stored in sourceMch
        AtomicReference<Path> sourceMch = new AtomicReference<>();
        return entries
                // Handle source annotations
                .peek(line -> {
                    if (line.startsWith("#source:")) {
                        int splitPos = line.indexOf(":");
                        String sourceEntry = line.substring(splitPos + 1);
                        sourceMch.set(Paths.get(sourceEntry));
                    }
                })
                // skip comments/annotation lines
                .filter(line -> !line.startsWith("#"))
                .map(entry -> new PredicateDump(entry, sourceMch.get()))
                .map(this::translate);
    }
}
