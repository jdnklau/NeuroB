package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.db.DbSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PredicateDbMigrationIT {

    @Test
    void shouldTranslatePredicateDumpToJson() throws IOException {
        // Resource dir of pdump files for the tests
        Path PDUMP_DIR = Paths.get(getClass().getClassLoader()
                .getResource("db/pdump/").getFile());
        // Source machine path of first pdump file
        Path firstSrc = Paths.get("machines/first.mch");
        // Source machine path of second pdump file
        Path secondSrc = Paths.get("machines/subdir/second.mch");

        // Set up temp dir to hold Json files
        Path tempDir = Files.createTempDirectory("neurob-it");
        // Json files that the migration should create
        Path firstJson = tempDir.resolve("first.json");
        Path secondJson = tempDir.resolve("subdir/second.json");

        PredicateDbFormat format = new JsonDbFormat();
        PredicateDbMigration migration = new PredicateDbMigration(new PredicateDumpFormat());

        // Expected values
        DbSample<BPredicate> firstEntry = new DbSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                firstSrc);
        DbSample<BPredicate> secondEntry = new DbSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                secondSrc);

        // Migrate
        migration.migrate(PDUMP_DIR, tempDir, format);

        List<DbSample> expectedFirst = new ArrayList<>();
        expectedFirst.add(firstEntry);
        expectedFirst.add(firstEntry);
        List<DbSample> expectedSecond = new ArrayList<>();
        expectedSecond.add(secondEntry);

        List<DbSample<BPredicate>> actualFirst =
                format.loadSamples(firstJson).collect(Collectors.toList());
        List<DbSample<BPredicate>> actualSecond =
                format.loadSamples(secondJson).collect(Collectors.toList());

        assertAll("Verify migration of predicate dump entries",
                () -> assertEquals(expectedFirst, actualFirst,
                        "Contents of first.json do not match"),
                () -> assertEquals(expectedSecond, actualSecond,
                        "Contents of second.json do not match"));
    }

    @Test
    void shouldTranslateJsonToPredicateDump() throws IOException {
        // Resource dir of json files for the tests
        Path JSON_DIR = Paths.get(getClass().getClassLoader()
                .getResource("db/json/").getFile());
        // Source machine path of first json file
        Path firstSrc = Paths.get("machines/first.mch");
        // Source machine path of second json file
        Path secondSrc = Paths.get("machines/subdir/second.mch");

        // Set up temp dir to hold Json files
        Path tempDir = Files.createTempDirectory("neurob-it");
        // Pdump files that the migration should create
        Path firstPdump = tempDir.resolve("first.pdump");
        Path secondPdump = tempDir.resolve("subdir/second.pdump");

        PredicateDbFormat format = new PredicateDumpFormat();
        PredicateDbMigration migration = new PredicateDbMigration(new JsonDbFormat());

        // Expected values
        DbSample<BPredicate> firstEntry = new DbSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                firstSrc);
        DbSample<BPredicate> secondEntry = new DbSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                secondSrc);

        // Migrate
        migration.migrate(JSON_DIR, tempDir, format);

        List<DbSample> expectedFirst = new ArrayList<>();
        expectedFirst.add(firstEntry);
        expectedFirst.add(firstEntry);
        List<DbSample> expectedSecond = new ArrayList<>();
        expectedSecond.add(secondEntry);

        List<DbSample<BPredicate>> actualFirst =
                format.loadSamples(firstPdump).collect(Collectors.toList());
        List<DbSample<BPredicate>> actualSecond =
                format.loadSamples(secondPdump).collect(Collectors.toList());

        assertAll("Verify migration of predicate dump entries",
                () -> assertEquals(expectedFirst, actualFirst,
                        "Contents of first.pdump do not match"),
                () -> assertEquals(expectedSecond, actualSecond,
                        "Contents of second.pdump do not match"));
    }
}