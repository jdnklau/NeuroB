package neurob.training.analysis;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.training.TrainingSetAnalyser;

public class TrainingAnalysisData {
	private ArrayList<Integer> trueLabelCounters; // For each class, have counter how many times it would be called
	private int filesSeen; // Counter for files seen in total
	private int emptyFilesSeen; // Counter for empty files

	private static final Logger log = LoggerFactory.getLogger(TrainingAnalysisData.class);
	
	/**
	 * 
	 * @param classes Number of classes to classify with training data
	 */
	public TrainingAnalysisData(int classes){
		trueLabelCounters = new ArrayList<Integer>(0);
		
		for(int i=0; i < classes; i++){
			trueLabelCounters.add(0);
		}
	}
	
	public void log(){
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			int relevantFiles = filesSeen-emptyFilesSeen;
			log.info("Files found: "+filesSeen);
			log.info("Of these were "+emptyFilesSeen+" seemingly empty");
			log.info("\t=> "+relevantFiles+" relevant files");
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
	
	/**
	 * @return An {@link ArrayList} of Integer counters how often each class (being the index) is represented. 
	 */
	public ArrayList<Integer> getTrueLabelCounters(){
		return trueLabelCounters;
	}
	
	/**
	 * Increases the counter of files seen by one.
	 * 
	 * @see #addEmptyFileSeen()
	 */
	public void addFileSeen(){
		filesSeen++;
	}
	
	public int getFilesSeen(){
		return filesSeen;
	}
	
	/**
	 * Increases the counter for empty files seen by one.
	 * 
	 * @see #addEmptyFileSeen()
	 */
	public void addEmptyFileSeen(){
		emptyFilesSeen++;
	}
	
	public int getEmptyFilesSeen(){
		return emptyFilesSeen;
	}
	
}
