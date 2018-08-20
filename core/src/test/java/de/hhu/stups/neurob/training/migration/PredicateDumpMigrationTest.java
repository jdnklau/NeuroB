package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDump;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDumpMigrationTest {

    private final Path MIGRATION_SOURCE =
            Paths.get(getClass()
                    .getClassLoader()
                    .getResource("db/migration/migrate.pdump")
                    .getFile());

    @Test
    public void shouldTranslatePredicateDumpIntoDbSample() {
        PredicateDump pdump = new PredicateDump("1.0,2.0,3.0,-1.0:predicate:PREDICATES");

        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Set up expected sample
        DbSample<BPredicate> expected = new DbSample<>(
                new BPredicate("predicate:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, -1.0));
        DbSample<BPredicate> actual = migration.translate(pdump);

        assertEquals(expected, actual,
                "Migration into DbSample failed");
    }

    @Test
    public void shouldTranslatePredicateDumpWithSourceIntoDbSampleWithSource() {
        PredicateDump pdump = new PredicateDump("1.0,2.0,3.0,-1.0:predicate:PREDICATES",
                Paths.get("nonexistent/mch"));

        PredicateDumpMigration migration = new PredicateDumpMigration();

        // Set up expected sample
        DbSample<BPredicate> expected = new DbSample<>(
                new BPredicate("predicate:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, -1.0),
                Paths.get("nonexistent/mch"));
        DbSample<BPredicate> actual = migration.translate(pdump);

        assertEquals(expected, actual,
                "Migration into DbSample failed");
    }

    @Test
    public void shouldStreamDbSamplesFromPDumpFile() throws IOException {
        PredicateDumpMigration migration = new PredicateDumpMigration();

        DbSample<BPredicate> sample0 = new DbSample<>(
                new BPredicate("null:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0));
        DbSample<BPredicate> sample1 = new DbSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("first/source/machine"));
        DbSample<BPredicate> sample2 = new DbSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("second/source/machine"));
        DbSample<BPredicate> sample3 = new DbSample<>(
                new BPredicate("third:PREDICATES"),
                new Labelling(1.0, 2.0, 3.0, 4.0),
                Paths.get("second/source/machine"));

        List<DbSample<BPredicate>> expected = new ArrayList<>();
        expected.add(sample0);
        expected.add(sample1);
        expected.add(sample2);
        expected.add(sample3);

        List<DbSample<BPredicate>> actual =
                migration.streamTranslatedSamples(MIGRATION_SOURCE).collect(Collectors.toList());

        assertEquals(expected, actual);
    }
}
