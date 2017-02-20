/**
 * 
 */
package neurob.training.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jannik
 *
 */
public class RegressionAnalysis implements TrainingAnalysisData {
	private int outputCount;
	private double[] minimum;
	private double[] maximum;
	private double[] sumOfAllSamples; // used for mean computation at logging
	// file information
	private int filesSeen;
	private int emptyFilesSeen;
	private int samplesSeen;

	private static final Logger log = LoggerFactory.getLogger(ClassificationAnalysis.class);
	
	public RegressionAnalysis(int numberOfOutputs){
		outputCount = numberOfOutputs;
		
		minimum = new double[numberOfOutputs];
		maximum = new double[numberOfOutputs];
		sumOfAllSamples = new double[numberOfOutputs];
		
		filesSeen = 0;
		emptyFilesSeen = 0;
		samplesSeen = 0;
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#log()
	 */
	@Override
	public void log() {
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			int relevantFiles = filesSeen-emptyFilesSeen;
			log.info("Files found: {}", filesSeen);
			log.info("Of these were {} seemingly empty", emptyFilesSeen);
			log.info("\t=> {} relevant files", relevantFiles);
		}
		
		// log boxplot values
		for(int i=0; i<outputCount; i++){
			log.info("Overview for first regression value:");
			log.info("\tMinimum: {}, Maximum: {}", minimum[i], maximum[i]);
			log.info("\tMean: {}", sumOfAllSamples[i]/outputCount);
		}
		
		log.info("*****************************");
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#countFileSeen()
	 */
	@Override
	public void countFileSeen() {
		filesSeen++;
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#getFilesCount()
	 */
	@Override
	public int getFilesCount() {
		return filesSeen;
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#countEmptyFileSeen()
	 */
	@Override
	public void countEmptyFileSeen() {
		emptyFilesSeen++;

	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#getEmptyFilesCount()
	 */
	@Override
	public int getEmptyFilesCount() {
		return emptyFilesSeen;
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#getSamplesCount()
	 */
	@Override
	public int getSamplesCount() {
		return samplesSeen;
	}

	/* (non-Javadoc)
	 * @see neurob.training.analysis.TrainingAnalysisData#analyseSample(double[], double[])
	 */
	@Override
	public void analyseSample(double[] features, double[] labels) {
		samplesSeen++;
		
		for(int i=0; i<outputCount; i++){
			
			// find minima and maxima
			if(labels[i] > maximum[i])
				maximum[i] = labels[i];
			else if(labels[i] < minimum[i])
				minimum[i] = labels[i];
			
			// add to total sum
			sumOfAllSamples[i] += labels[i];
		}

	}

}
