package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.KodkodBackend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.SmtBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.TheoryFeatures;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.core.labelling.Labelling;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.CsvFormat;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
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
import static org.junit.jupiter.api.Assertions.fail;

public class PredicateDbMigrationIT {

    /**
     * Backends used by this test suite
     */
    private final Backend[] BACKENDS_USED = {
            new ProBBackend(),
            new KodkodBackend(),
            new Z3Backend(),
            new SmtBackend(),
    };

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
        Path firstJson = tempDir.resolve("machines/first.json");
        Path secondJson = tempDir.resolve("machines/subdir/second.json");

        PredicateDbFormat<? extends Labelling> format = new JsonDbFormat(BACKENDS_USED);
        PredicateDbMigration migration = new PredicateDbMigration(new PredicateDumpFormat());

        // Expected values
        TrainingSample<BPredicate, ? extends Labelling> firstEntry = new TrainingSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                firstSrc);
        TrainingSample<BPredicate, ? extends Labelling> secondEntry = new TrainingSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                secondSrc);

        // Migrate
        migration.migrate(PDUMP_DIR, tempDir, format);

        List<TrainingSample<BPredicate, ? extends Labelling>> expectedFirst = new ArrayList<>();
        expectedFirst.add(firstEntry);
        expectedFirst.add(firstEntry);
        List<TrainingSample<BPredicate, ? extends Labelling>> expectedSecond = new ArrayList<>();
        expectedSecond.add(secondEntry);

        List<TrainingSample<BPredicate, ? extends Labelling>> actualFirst =
                format.loadSamples(firstJson).collect(Collectors.toList());
        List<TrainingSample<BPredicate, ? extends Labelling>> actualSecond =
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
        Path firstPdump = tempDir.resolve("machines/first.pdump");
        Path secondPdump = tempDir.resolve("machines/subdir/second.pdump");

        PredicateDbFormat<? extends Labelling> format = new PredicateDumpFormat();
        PredicateDbMigration migration = new PredicateDbMigration(new JsonDbFormat(BACKENDS_USED));

        // Expected values
        TrainingSample<BPredicate, ? extends Labelling> firstEntry = new TrainingSample<>(
                new BPredicate("first:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                firstSrc);
        TrainingSample<BPredicate, ? extends Labelling> secondEntry = new TrainingSample<>(
                new BPredicate("second:PREDICATES"),
                new Labelling(1., 2., 3., 4.),
                secondSrc);

        // Migrate
        migration.migrate(JSON_DIR, tempDir, format);

        List<TrainingSample<BPredicate, ? extends Labelling>> expectedFirst = new ArrayList<>();
        expectedFirst.add(firstEntry);
        expectedFirst.add(firstEntry);
        List<TrainingSample<BPredicate, ? extends Labelling>> expectedSecond = new ArrayList<>();
        expectedSecond.add(secondEntry);

        List<TrainingSample<BPredicate, ? extends Labelling>> actualFirst =
                format.loadSamples(firstPdump).collect(Collectors.toList());
        List<TrainingSample<BPredicate, ? extends Labelling>> actualSecond =
                format.loadSamples(secondPdump).collect(Collectors.toList());

        assertAll("Verify migration of predicate dump entries",
                () -> assertEquals(expectedFirst, actualFirst,
                        "Contents of first.pdump do not match"),
                () -> assertEquals(expectedSecond, actualSecond,
                        "Contents of second.pdump do not match"));
    }

    @Test
    void shouldTranslateJsonToTheoryFeaturesCsv() throws IOException {
        // Resource dir of json and mch files for the tests
        Path JSON_DIR = Paths.get(getClass().getClassLoader().getResource("db/json/").getFile());
        Path MCH_DIR = Paths.get(getClass().getClassLoader().getResource("machines/").getFile());

        // Create tmp dir
        Path tempDir = Files.createTempDirectory("neurob-it");

        // Set up directory hierarchy
        Path tmpJson = tempDir.resolve("json");
        Path tmpCsv = tempDir.resolve("csv");

        // Build Json subdirectory and copy files into
        Files.createDirectories(tmpJson);
//        Path featuresCheck = tmpJson.resolve("features_check.json");
        Path formulaeGen = tmpJson.resolve("formulae_generation.json");
//        Files.copy(JSON_DIR.resolve("features_check.json"), featuresCheck);
        Files.copy(JSON_DIR.resolve("formulae_generation.json"), formulaeGen);

        Backend[] backends = new Backend[]{
                new ProBBackend(),
                new Z3Backend(),
        };
        JsonDbFormat dbFormat = new JsonDbFormat(backends);


        // Migration step
        PredicateDbMigration migration = new PredicateDbMigration(dbFormat);
        migration.migrate(tmpJson, tmpCsv, MCH_DIR,
                TheoryFeatures::new,
                dbEntry -> new BackendClassification(dbEntry.getPredicate(), dbEntry.getBackendsUsed(), dbEntry.getResults()),
                new CsvFormat(TheoryFeatures.featureDimension, 1));

        Path expectedCsv = Paths.get(getClass().getClassLoader().getResource("formats/csv/formulae_generation.csv").getFile());
        List<String> expected = Files.readAllLines(expectedCsv);
        List<String> actual = Files.readAllLines(tmpCsv.resolve("formulae_generation.csv"));

        assertEquals(expected, actual,
                "CSV contents do not match");
    }


}
