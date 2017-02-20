package neurob.training.analysis;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrainingAnalysisData implements TrainingAnalysisDataContainer {
	private ArrayList<Integer> trueLabelCounters; // For each class, have counter how many times it would be called
	private int filesSeen; // Counter for files seen in total
	private int emptyFilesSeen; // Counter for empty files
	private int samples; // Number of samples seen

	private static final Logger log = LoggerFactory.getLogger(TrainingAnalysisData.class);
	
	/**
	 * 
	 * @param classes Number of classes to classify with training data
	 */
	public TrainingAnalysisData(int classes){
		trueLabelCounters = new ArrayList<Integer>(0);
		filesSeen = 0;
		emptyFilesSeen = 0;
		samples = 0;
		
		for(int i=0; i < classes; i++){
			trueLabelCounters.add(0);
		}
	}

	@Override
	public void log(){
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			int relevantFiles = filesSeen-emptyFilesSeen;
			log.info("Files found: {}", filesSeen);
			log.info("Of these were {} seemingly empty", emptyFilesSeen);
			log.info("\t=> {} relevant files", relevantFiles);
		}
		// list classification mappings
		log.info("Overview of class representation:");
		int dataCount = 0;
		for(int i = 0; i < trueLabelCounters.size(); i++){
			int classSamples = trueLabelCounters.get(i);
			dataCount += classSamples;
			log.info("\tClass {} is represented by {} samples", i, classSamples);
		}
		log.info("{} samples in total", dataCount);
		
		log.info("*****************************");
	}
	
	/**
	 * Increases the counter for the specified classification by one
	 * @param clss Class of true label.
	 */
	public void countEntryForClass(int clss){
		int c = trueLabelCounters.get(clss);
		trueLabelCounters.set(clss, c+1);
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
	 * @return An {@link ArrayList} of Integer counters how often each class (being the index) is represented. 
	 */
	public ArrayList<Integer> getTrueLabelCounters(){
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
	
}
