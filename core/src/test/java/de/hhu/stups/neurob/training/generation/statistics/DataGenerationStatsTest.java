package de.hhu.stups.neurob.training.generation.statistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataGenerationStatsTest {

    @Test
    public void shouldInitWithZero() {
        DataGenerationStats stats = new DataGenerationStats();

        assertAll("Should all be 0",
                () -> assertEquals(0, stats.getFilesCreated(),
                        "Should be 0: files created"),
                () -> assertEquals(0, stats.getFilesSeen(),
                        "Should be 0: files seen"),
                () -> assertEquals(0, stats.getFilesWithErrors(),
                        "Should be 0: files inaccessible"),
                () -> assertEquals(0, stats.getSamplesWritten(),
                        "Should be 0: samples written"),
                () -> assertEquals(0, stats.getSamplesFailed(),
                        "Should be 0: samples failed"));
    }

    @Test
    public void shouldUseConstructorParams() {
        DataGenerationStats stats = new DataGenerationStats(1, 2, 5, 3, 4);

        assertAll(
                () -> assertEquals(2, stats.getFilesCreated(),
                        "Should be 2: files created"),
                () -> assertEquals(1, stats.getFilesSeen(),
                        "Should be 1: files seen"),
                () -> assertEquals(5, stats.getFilesWithErrors(),
                        "Should be 5: files inaccessible"),
                () -> assertEquals(3, stats.getSamplesWritten(),
                        "Should be 3: samples written"),
                () -> assertEquals(4, stats.getSamplesFailed(),
                        "Should be 4: samples failed"));
    }

    @Test
    public void shouldIncreaseFilesSeen() {
        DataGenerationStats stats = new DataGenerationStats();

        int expected = 3;
        stats.increaseFilesSeen();
        int actual = stats.increaseFilesSeen(2);

        assertAll("Files seen should be increased",
                () -> assertEquals(expected, actual,
                        "Return type does not match"),
                () -> assertEquals(expected, stats.getFilesSeen(),
                        "Getter does not match"));
    }

    @Test
    public void shouldIncreaseFilesCreated() {
        DataGenerationStats stats = new DataGenerationStats();

        int expected = 3;
        stats.increaseFilesCreated();
        int actual = stats.increaseFilesCreated(2);

        assertAll("Files created should be increased",
                () -> assertEquals(expected, actual,
                        "Return type does not match"),
                () -> assertEquals(expected, stats.getFilesCreated(),
                        "Getter does not match"));
    }

    @Test
    public void shouldIncreaseFilesWithErrors() {
        DataGenerationStats stats = new DataGenerationStats();

        int expected = 3;
        stats.increaseFilesWithErrors();
        int actual = stats.increaseFilesWithErrors(2);

        assertAll("Files inaccessible should be increased",
                () -> assertEquals(expected, actual,
                        "Return type does not match"),
                () -> assertEquals(expected, stats.getFilesWithErrors(),
                        "Getter does not match"));
    }

    @Test
    public void shouldIncreaseSamplesWritten() {
        DataGenerationStats stats = new DataGenerationStats();

        int expected = 3;
        stats.increaseSamplesWritten();
        int actual = stats.increaseSamplesWritten(2);

        assertAll("Samples written should be increased",
                () -> assertEquals(expected, actual,
                        "Return type does not match"),
                () -> assertEquals(expected, stats.getSamplesWritten(),
                        "Getter does not match"));
    }

    @Test
    public void shouldIncreaseSamplesFailed() {
        DataGenerationStats stats = new DataGenerationStats();

        int expected = 3;
        stats.increaseSamplesFailed();
        int actual = stats.increaseSamplesFailed(2);

        assertAll("Samples failed should be increased",
                () -> assertEquals(expected, actual,
                        "Return type does not match"),
                () -> assertEquals(expected, stats.getSamplesFailed(),
                        "Getter does not match"));
    }

    @Test
    public void shouldSumIndividualValuesWhenMergingTwoStatistics() {
        DataGenerationStats stats1 = new DataGenerationStats(1, 2, 9, 3, 4);
        DataGenerationStats stats2 = new DataGenerationStats(5, 6, 10, 7, 8);

        DataGenerationStats stats = stats1.mergeWith(stats2);

        assertAll(
                () -> assertEquals(8, stats.getFilesCreated(),
                        "Should be 8: files created"),
                () -> assertEquals(6, stats.getFilesSeen(),
                        "Should be 6: files seen"),
                () -> assertEquals(19, stats.getFilesWithErrors(),
                        "Should be 19: files inaccessible"),
                () -> assertEquals(10, stats.getSamplesWritten(),
                        "Should be 10: samples written"),
                () -> assertEquals(12, stats.getSamplesFailed(),
                        "Should be 12: samples failed"));

    }

    @Test
    public void shouldPrintCorrectNumbersInStringRepresentation() {
        DataGenerationStats stats = new DataGenerationStats(10, 4, 6, 20, 2);

        String expected = "Files seen: 10; "
                          + "Files that lead to errors: 6; "
                          + "Training files created: 4; "
                          + "Training samples written: 20; "
                          + "Training samples that lead to errors: 2";
        String actual = stats.toString();

        assertEquals(expected, actual);
    }
}
