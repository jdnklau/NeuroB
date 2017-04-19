package neurob.training.analysis;

import java.util.Arrays;

import neurob.training.generators.interfaces.LabelGenerator;

public interface TrainingAnalysisData {
	
	/**
	 * Returns the evaluation of the seen training data as string message.
	 * @return The analysis results in string representation
	 */
	public String getStatistics();
	
	/**
	 * Evaluates the seen samples. This processes some measurements which can not be processed while seeing the data,
	 * but can only be measured after seeing the whole data set.
	 * @return Reference to this instance.
	 */
	public TrainingAnalysisData evaluateAllSamples();
	
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
	
	/**
	 * Takes a sample as String, e.g. a line from a training data file,
	 * and analyses it.
	 * Also counts the entry towards the output of {@link #getSamplesCount()}
	 * <p>
	 * Note that internally, this calls {@link #analyseSample(double[], double[])}.
	 * @param sampleString
	 */
	default public void analyseTrainingDataSample(String sampleString){
		String[] sample = sampleString.split(":");
		double[] features = Arrays.stream(sample[0].split(","))
				.mapToDouble(Double::parseDouble).toArray();
		double[] labels = Arrays.stream(sample[1].split(","))
				.mapToDouble(Double::parseDouble).toArray();
		this.analyseSample(features, labels);
	}
	
	/**
	 * After analysing the data and calling {@link #evaluateAllSamples()}, decides for a given labelling
	 * whether it should be kept in the data set or can be truncated.
	 * <p>
	 * This may be a probabilistic approach, deciding the keepability of a sample by chance. However, promise is to
	 * behave deterministically iff run over the same data twice with no parallelisation.
	 * 
	 * @param trainingLabels 
	 * 			Labels on which to decide the trimming.
	 * 			Those are training labels, in regards to {@link LabelGenerator#getTrainingLabelDimension()}.
	 * 			I.e. for classification problems, this array only contains a single value: the class.
	 * @return true iff sample can be disregarded.
	 */
	public boolean canSampleBeTrimmed(double[] trainingLabels);
}
