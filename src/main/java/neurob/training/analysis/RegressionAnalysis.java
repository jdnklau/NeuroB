/**
 * 
 */
package neurob.training.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jannik
 *
 */
public class RegressionAnalysis implements TrainingAnalysisData {
	protected final int outputCount;
	protected double[] sumOfAllSamples; // used for mean computation at logging
	protected List<List<Double>> samples;
	// file information
	protected int filesSeen;
	protected int emptyFilesSeen;
	protected int samplesSeen;
	// evaluation results
	protected double[] minimum;
	protected double[] firstQuartile;
	protected double[] median;
	protected double[] thirdQuartile;
	protected double[] maximum;
	protected double[] mean;
	protected double[] variance;
	protected double[] stddev;
	

	private static final Logger log = LoggerFactory.getLogger(RegressionAnalysis.class);
	
	public RegressionAnalysis(int numberOfOutputs){
		outputCount = numberOfOutputs;
		
		sumOfAllSamples = new double[numberOfOutputs];
		
		samples = new ArrayList<List<Double>>(numberOfOutputs);
		for(int i=0; i<numberOfOutputs; i++){
			samples.add(i, new ArrayList<Double>());
		}
		
		filesSeen = 0;
		emptyFilesSeen = 0;
		samplesSeen = 0;
		
		minimum = new double[numberOfOutputs];
		firstQuartile = new double[numberOfOutputs];
		median = new double[numberOfOutputs];
		thirdQuartile = new double[numberOfOutputs];
		maximum = new double[numberOfOutputs];
		mean = new double[numberOfOutputs];
		variance = new double[numberOfOutputs];
		stddev = new double[numberOfOutputs];
		
		// initial values for min and max
		for(int i=0; i<numberOfOutputs; i++){
			minimum[i] = Integer.MAX_VALUE;// any first sample will be smaller, defining the new minimum
			maximum[i] = Integer.MIN_VALUE;// any first sample will be greater, defining the new maximum
		}
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
		log.info("Samples seen: {}", samplesSeen);
		// log boxplot values
		for(int i=0; i<outputCount; i++){
			log.info("Overview for #{} regression value:", i);
			log.info("\tMinimum: {}, Maximum: {}", minimum[i], maximum[i]);
			log.info("\tMean: {}", mean[i]);
			log.info("\tVariance: {}", variance[i]);
			log.info("\tStandard deviation: {}", stddev[i]);
			log.info("\tMedian: {}", median[i]);
			log.info("\tFirst Quartile: {}, Third Quartile: {}", firstQuartile[i], thirdQuartile[i]);
		}
		
		log.info("*****************************");
	}
	
	@Override
	public TrainingAnalysisData evaluateAllSamples() {
		// sort output lists
		for(int i=0; i<outputCount; i++){
			List<Double> samps = samples.get(i);
			Collections.sort(samps);
			int sampsCount = samps.size();
			
			// minimum and maximum
			minimum[i] = samps.get(0);
			maximum[i] = samps.get(sampsCount-1);
			
			// mean
			mean[i] = sumOfAllSamples[i]/sampsCount;
			
			// variance and std dev
			double sqrsum = 0;
			for(Double d : samps){
				sqrsum += Math.pow(d-mean[i], 2); // for each data point: squared distance to mean
			}
			variance[i] = sqrsum/sampsCount;
			stddev[i] = Math.sqrt(variance[i]);
						
			// median, first and third quartile
			int medianIndex = sampsCount/2;
			int firstQIndex = (int) (sampsCount*0.25);
			int thirdQIndex = (int) (sampsCount*0.75);
			
			median[i] = (sampsCount %2 == 1) ?
					samps.get(medianIndex) : (samps.get(medianIndex-1) + samps.get(medianIndex))/2.0;
			firstQuartile[i] = (sampsCount %4 != 0) ?
					samps.get(firstQIndex) : (samps.get(firstQIndex-1) + samps.get(firstQIndex))/2.0;
			thirdQuartile[i] = (sampsCount %4 != 0) ?
					samps.get(thirdQIndex) : (samps.get(thirdQIndex-1) + samps.get(thirdQIndex))/2.0;
		}
		
		return this;
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
			
			// add to total sum
			sumOfAllSamples[i] += labels[i];
			
			// and add to sample collection
			samples.get(i).add(labels[i]);
		}

	}
	
	/**
	 * Returns an array holding the extrema, first, second, and third quartile.
	 * <p>
	 * The indices map to the values in this order: 0 minimum, 1 first quartile, 2 median (second quartile),
	 * 3 third quartile, 4 maximum.
	 * @param output Identifier for which output the data will be given
	 * @return 
	 */
	public double[] getQuartiles(int output){
		final int i = output;
		return new double[]{minimum[i], firstQuartile[i], median[i], thirdQuartile[i], maximum[i]};
	}
	
	@Override
	public boolean canSampleBeTrimmed(double[] trainingLabels) {
		// NOTE: In regression we do not need trimming.
		return false;
	}

}
