package neurob.training.analysis;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.training.generators.labelling.PredicateDumpLabelGenerator;

public class PredicateDumpAnalysis extends RegressionAnalysis {
	// data over solvers
	private long[] negSamples;
	private final static int solversAccountedFor = PredicateDumpLabelGenerator.solversAccountedFor;
	private double[] decidabilityDist; // Distribution of decidability of samples between solvers
	

	/**
	 * Count of samples pairwise decidable by two solvers.
	 * Calculated as upper triangular matrix
	 * <br>Indices through each dimension are: 0 ProB, 1 KodKod, 2 Z3, 3 ProB+Z3
	 */
	private int[][] decidabilityMatrix;
	
	// logger
	private static final Logger log = LoggerFactory.getLogger(PredicateDumpAnalysis.class);
	
	public PredicateDumpAnalysis() {
		super(solversAccountedFor);
		samplesSeen=0;
		
		decidabilityDist = new double[solversAccountedFor];
		decidabilityMatrix = new int[solversAccountedFor][solversAccountedFor];
		
		// Note: using 4 values here, but generating only data for first three solvers
		negSamples = new long[]{0L,0L,0L,0L};
	}

	@Override
	public void log() {
		final String[] solverNames = new String[]{"ProB", "KodKod", "Z3", "ProB+Z3"};
		
		log.info("Analysis of training data");
		
		if(filesSeen > 0){
			int relevantFiles = filesSeen-emptyFilesSeen;
			log.info("Files found: {}", filesSeen);
			log.info("Of these were {} seemingly empty", emptyFilesSeen);
			log.info("\t=> {} relevant files", relevantFiles);
		}
		log.info("Samples total: {}", samplesSeen);
		
		// log ifo per solver
		for(int i=0; i<solversAccountedFor; i++){
			log.info("# Overview for {}", solverNames[i]);
			log.info("{} of {} samples could be decided ({} of all samples):",
					decidabilityMatrix[i][i], samplesSeen, decidabilityDist[i]);
			
			// cross solver information
			for(int j=0; j<solversAccountedFor; j++){
				if(i!=j){
					log.info("\t{} could also be decided by {} ({} of decided samples)",
							decidabilityMatrix[i][j], solverNames[j],
							decidabilityMatrix[i][j]/(double)decidabilityMatrix[i][i]);
				}
			}
			// boxplot values
			log.info("Boxplot values:");
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
		// boxplot values
		super.evaluateAllSamples();
		
		// calculate decidability distribution
		for(int i=0; i<solversAccountedFor; i++){
			decidabilityDist[i] = decidabilityMatrix[i][i]/(double)samplesSeen;
		}
		
		return this;
	}

	public double[] getDecidabilityDist() {
		return decidabilityDist;
	}

	public int[][] getDecidabilityMatrix() {
		return decidabilityMatrix;
	}

	@Override
	public void analyseSample(double[] features, double[] labels) {
		// for each solver
		boolean[] couldDecide = {false, false, false, false}; // to compare which solvers could decide the same sample
		for(int s=0; s<labels.length; s++){
			double l=labels[s];
			
			/*
			 * negative values are counted as negSamples.
			 * Otherwise, the time the solver needed is added to sampleSums
			 */
			
			if(l<0){
				negSamples[s]++;
			}
			else {
				sumOfAllSamples[s]+=l;
				samples.get(s).add(l);
				couldDecide[s] = true;
			}
		}
		
		samplesSeen++; // count sample total
		addToDecidabilityMatrix(couldDecide);
	}
	
	private void addToDecidabilityMatrix(boolean[] couldDecide) {
		for(int i=0; i<solversAccountedFor; i++){
			for(int j=i; j<solversAccountedFor; j++){
				if(couldDecide[i] && couldDecide[j]){
					decidabilityMatrix[i][j]++;
					if(i!=j)
						decidabilityMatrix[j][i]++;
				}
			}
		}
	}

	@Override
	public void analyseTrainingDataSample(String sampleString) {
		if(sampleString.startsWith("#"))
			return; // ignore commented lines
		double[] labels = Arrays.stream(sampleString.split(":")[0].split(","))
				.mapToDouble(Double::parseDouble).toArray();
		analyseSample(null, labels);
	}
	
	@Override
	public boolean canSampleBeTrimmed(double[] trainingLabels) {
		// NOTE: In regression we do not need trimming.
		return false;
	}

}
