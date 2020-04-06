package neurob.training.analysis;

import java.util.Random;


public class ClassificationAnalysis implements TrainingAnalysisData {
	private int[] trueLabelCounters; // For each class, have counter how many times it would be called
	private int filesSeen; // Counter for files seen in total
	private int emptyFilesSeen; // Counter for empty files
	private int samples; // Number of samples seen
	public final int classCount;
	private double[] classDist;
	private double[] classTrimChance;

	private final Random rng = new Random(123);

	/**
	 *
	 * @param classCount Number of classCount to classify with training data
	 */
	public ClassificationAnalysis(int classes){
		filesSeen = 0;
		emptyFilesSeen = 0;
		samples = 0;

		this.classCount=classes;

		trueLabelCounters = new int[classes];
		classDist = new double[classes];
		classTrimChance = new double[classes];
	}

	@Override
	public String getStatistics() {
		StringBuilder res = new StringBuilder();

		if(filesSeen > 0){
			res.append("Files found: ").append(filesSeen);
			res.append("\nOf these were ").append(emptyFilesSeen).append("seemingly empty\n");
		}
		// list classification mappings
		res.append("Overview of class representation:\n");
		int dataCount = 0;
		for(int i = 0; i < classCount; i++){
			int classSamples = trueLabelCounters[i];
			dataCount += classSamples;
			res.append("\tClass ").append(i)
				.append(" is represented by ").append(classSamples)
				.append(" samples (").append(classDist[i]).append(" of all samples)\n");
		}
		res.append(dataCount).append(" samples in total");

		return res.toString();
	}

	/**
	 * Increases the counter for the specified classification by one
	 * @param clss Class of true label.
	 */
	public void countEntryForClass(int clss){
		trueLabelCounters[clss]++;
	}

	@Override
	public void analyseSample(double[] features, double[] labels) {
		// TODO: For now only takes labels into account. Maybe add TSne or PCA or stuff like that?
		int clss = (int) Math.round(labels[0]); // TODO: make this beautiful, as this is plain ugly code
		countEntryForClass(clss);
		samples++;
	}

	@Override
	public int getSamplesCount(){
		return samples;
	}

	/**
	 * @return An Array of Integer counters how often each class (being the index) is represented.
	 */
	public int[] getTrueLabelCounters(){
		return trueLabelCounters;
	}

	@Override
	public void countFileSeen(){
		filesSeen++;
	}

	@Override
	public int getFilesCount(){
		return filesSeen;
	}

	@Override
	public void countEmptyFileSeen(){
		emptyFilesSeen++;
	}

	@Override
	public int getEmptyFilesCount(){
		return emptyFilesSeen;
	}

	/**
	 * @return Probability distribution of the different classes.
	 */
	public double[] getClassDistribution(){
		return classDist;
	}

	@Override
	public TrainingAnalysisData evaluateAllSamples() {
		// calculate class distribution
		classDist = new double[classCount];
		double smallestClass = 2; // as probabilities should always in [0,1], 2 is definitely not the minimum
		for(int i=0; i<classCount; i++){
			classDist[i] = trueLabelCounters[i]/(double)samples;
			if(smallestClass > classDist[i])
				smallestClass = classDist[i];
		}

		// set probabilities for disregarding samples
		for(int i=0; i<classCount; i++){
			classTrimChance[i] = 1-smallestClass/classDist[i];
		}

		return this;
	}

	@Override
	public boolean canSampleBeTrimmed(double[] trainingLabels) {
		int clss = (int) trainingLabels[0];

		double chance = rng.nextDouble();

		return chance <= classTrimChance[clss];
	}

}
