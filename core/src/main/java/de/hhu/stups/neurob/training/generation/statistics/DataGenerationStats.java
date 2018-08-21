package de.hhu.stups.neurob.training.generation.statistics;

/**
 * Statistics class holding information about results from
 * training set generation.
 */
public class DataGenerationStats {
    /** Number of total files seen by this generation step */
    private int filesSeen;
    /** Number of distinct files created by this generation step */
    private int filesCreated;
    /** Number of distinct files that yielded errors during training generation */
    private int filesWithErrors;
    /** Number of samples that were successfully written to a file */
    private int samplesWritten;
    /** Number of samples that failed to be created due to encountered errors */
    private int samplesFailed;

    public DataGenerationStats() {
        this(0, 0, 0, 0, 0);
    }

    /**
     * @param filesSeen Number of distinct files used by this generation step
     * @param filesCreated Number of distinct files created by this generation step
     * @param filesInaccessible Number of distinct files that were inaccessible during
     *         training generation
     * @param samplesWritten Number of samples that were successfully written to a file
     * @param samplesFailed Number of samples that failed to be created due to encountered
     */
    public DataGenerationStats(int filesSeen, int filesCreated, int filesInaccessible, int samplesWritten, int samplesFailed) {
        this.filesSeen = filesSeen;
        this.filesCreated = filesCreated;
        this.filesWithErrors = filesInaccessible;
        this.samplesWritten = samplesWritten;
        this.samplesFailed = samplesFailed;
    }

    public int getFilesSeen() {
        return filesSeen;
    }

    /**
     * Increase number of distinct files used by this generation step.
     *
     * @return New number of files used.
     */
    public int increaseFilesSeen() {
        return increaseFilesSeen(1);
    }

    /**
     * Increase number of distinct files used by this generation step.
     *
     * @param amount How many files were seen additionally
     *
     * @return New number of files used.
     */
    public int increaseFilesSeen(int amount) {
        filesSeen += amount;
        return filesSeen;
    }

    public int getFilesCreated() {
        return filesCreated;
    }

    /**
     * Increase number of distinct files created by this generation step.
     *
     * @return New number of files created
     */
    public int increaseFilesCreated() {
        return increaseFilesCreated(1);
    }

    /**
     * Increase number of distinct files created by this generation step.
     *
     * @param amount How many additional files were created
     *
     * @return New number of files created
     */
    public int increaseFilesCreated(int amount) {
        filesCreated += amount;
        return filesCreated;
    }

    public int getFilesWithErrors() {
        return filesWithErrors;
    }

    /**
     * Number of distinct files that yielded errors during training generation
     *
     * @return New number of files created
     */
    public int increaseFilesWithErrors() {
        return increaseFilesWithErrors(1);
    }

    /**
     * Number of distinct files that yielded errors during training generation
     *
     * @param amount How many additional files were inaccessible
     *
     * @return New number of inaccessible files
     */
    public int increaseFilesWithErrors(int amount) {
        filesWithErrors += amount;
        return filesWithErrors;
    }

    public int getSamplesWritten() {
        return samplesWritten;
    }

    /**
     * Increase number of samples that were successfully written to a file.
     *
     * @return New number of samples written
     */
    public int increaseSamplesWritten() {
        return increaseSamplesWritten(1);
    }

    /**
     * Increase number of samples that were successfully written to a file.
     *
     * @param amount How many additional samples were successfully written
     *
     * @return New number of samples written
     */
    public int increaseSamplesWritten(int amount) {
        samplesWritten += amount;
        return samplesWritten;
    }

    public int getSamplesFailed() {
        return samplesFailed;
    }

    /**
     * Increase number of samples that failed to be created due to encountered errors.
     *
     * @return New number of samples that encountered errors
     */
    public int increaseSamplesFailed() {
        return increaseSamplesFailed(1);
    }

    /**
     * Increase number of samples that failed to be created due to encountered errors.
     *
     * @param amount How many additional samples caused an error
     *
     * @return New number of samples that encountered errors
     */
    public int increaseSamplesFailed(int amount) {
        samplesFailed += amount;
        return samplesFailed;
    }

    /**
     * Merges the given statistics into this one, i.e. adding up the values.
     *
     * @param other
     *
     * @return
     */
    public DataGenerationStats mergeWith(DataGenerationStats other) {
        filesSeen += other.filesSeen;
        filesCreated += other.filesCreated;
        filesWithErrors += other.filesWithErrors;
        samplesWritten += other.samplesWritten;
        samplesFailed += other.samplesFailed;

        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Files seen: ").append(filesSeen).append("; ")
                .append("Files that lead to errors: ").append(filesWithErrors).append("; ")
                .append("Training files created: ").append(filesCreated).append("; ")
                .append("Training samples written: ").append(samplesWritten).append("; ")
                .append("Training samples that lead to errors: ").append(samplesFailed);

        return builder.toString();
    }
}
