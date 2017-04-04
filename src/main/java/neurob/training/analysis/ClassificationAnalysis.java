package neurob.training.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClassificationAnalysis implements TrainingAnalysisData {
	private int[] trueLabelCounters; // For each class, have counter how many times it would be called
	private int filesSeen; // Counter for files seen in total
	private int emptyFilesSeen; // Counter for empty files
	private int samples; // Number of samples seen
	public final int classCount;
	private double[] classDist;

	private static final Logger log = LoggerFactory.getLogger(ClassificationAnalysis.class);
	
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
	}

	@Override
	public void log(){
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			log.info("Files found: {}", filesSeen);
			log.info("Of these were {} seemingly empty", emptyFilesSeen);
		}
		// list classification mappings
		log.info("Overview of class representation:");
		int dataCount = 0;
		for(int i = 0; i < classCount; i++){
			int classSamples = trueLabelCounters[i];
			dataCount += classSamples;
			log.info("\tClass {} is represented by {} samples ({} of all samples)", i, classSamples, classDist[i]);
		}
		log.info("{} samples in total", dataCount);
		
		log.info("*****************************");
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
		for(int i=0; i<classCount; i++){
			classDist[i] = trueLabelCounters[i]/(double)samples;
		}
		
		return this;
	}
	
}
