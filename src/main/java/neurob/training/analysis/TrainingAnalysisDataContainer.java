package neurob.training.analysis;

public interface TrainingAnalysisDataContainer {
	
	/**
	 * Logs the stored results of the analysis.
	 */
	public void log();
	
	/**
	 * Increases the counter of files seen by one.
	 */
	public void countFileSeen();
	
	/**
	 * 
	 * @return Number of counted files
	 */
	public int getFilesCount();
	
	/**
	 * Increases the counter of files seen by one.
	 * <p>
	 * Note: This does not increase the file counter in total;
	 * for this one has to additionally call {@link #countFileSeen()}
	 * 
	 * @see #countFileSeen()
	 */
	public void countEmptyFileSeen();
	
	/**
	 * 
	 * @return Number of counted, empty files
	 */
	public int getEmptyFilesCount();
	
	/**
	 * 
	 * @return Number of entries analysed
	 */
	public int getSamplesCount();
	
	/**
	 * Proceeds with the analysis of the input entry. 
	 * Also counts the entry towards the output of {@link #getSamplesCount()}
	 * @param features
	 * @param labels
	 * @see #getSamplesCount()
	 */
	public void analyseSample(double[] features, double[] labels);
}
