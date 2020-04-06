package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.testharness.TestMachines;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PredicateDbAnalyserIT {

    @Test
    void shouldAnalyseDirectory() throws IOException {
        // Set up temporary directory
        Path tmpDir = Files.createTempDirectory("neurob-it");
        Files.createDirectories(tmpDir.resolve("subdir"));
        // Copy files
        Path jsonDb = Paths.get(TestMachines.getDbPath("json"));
        String[] files = {
                "formulae_generation.json",
                "first.json",
                "corrupt.json",
                "contradiction.json",
                "subdir/second.json",
        };
        for (String f : files)  {
            Files.copy(jsonDb.resolve(f), tmpDir.resolve(f));
        }

        PredicateDbAnalyser analyser = new PredicateDbAnalyser(new JsonDbFormat());
        PredDbAnalysis analysis = analyser.analyse(tmpDir);

        assertAll(
                // Counting statistics
                () -> assertEquals(4, analysis.getBMachineCount(),
                        "Number of B machines does not match"),
                () -> assertEquals(Long.valueOf(154), analysis.getPredCount(),
                        "Number of predicates does not match"),
                // Contradictions
                () -> assertEquals(Long.valueOf(2), analysis.getContradictionCount(),
                        "Number of contradictions does not match")
        );

    }

}
