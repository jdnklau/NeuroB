package neurob.training.analysis;

import java.util.Arrays;

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
	
	public PredicateDumpAnalysis() {
		super(solversAccountedFor);
		samplesSeen=0;
		
		decidabilityDist = new double[solversAccountedFor];
		decidabilityMatrix = new int[solversAccountedFor][solversAccountedFor];
		
		// Note: using 4 values here, but generating only data for first three solvers
		negSamples = new long[]{0L,0L,0L,0L};
	}
	
	@Override
	public String getStatistics() {
		// set up names of solvers
		final String[] solverNames = new String[solversAccountedFor];
		for(int i=0; i<solversAccountedFor; i++){
			solverNames[i] = PredicateDumpLabelGenerator.solverOrder[i].toString();
		}

		StringBuilder res = new StringBuilder();

		if(filesSeen > 0){
		    res.append("Files found: ").append(filesSeen);
		    res.append("\nOf these were ").append(emptyFilesSeen).append(" seemingly empty\n");
		}
		res.append("Samples seen: ").append(samplesSeen);
		
		// info per solver
		for(int i=0; i<solversAccountedFor; i++){
		    res.append("# Overview for ").append(solverNames[i]).append("\n");

		    res.append(decidabilityMatrix[i][i]).append(" of ").append(samplesSeen)
		        .append(" samples could be decided (")
		        .append(decidabilityDist[i]).append(" of all samples):\n");

		    // cross solver information
		    for(int j=0; j<solversAccountedFor; j++){
		        if(i!=j){
		            res.append("\t").append(decidabilityMatrix[i][j])
		                .append(" could also be decided by ").append(solverNames[j])
		                .append(" (")
		                    .append(decidabilityMatrix[i][j]/(double)decidabilityMatrix[i][i])
		                .append(" of decided samples)\n");
		        }
		    }
		    
		    // Boxplot values
		    res.append("\n\tMinimum: ").append(minimum[i])
		            .append(", Maximum: ").append(maximum[i])
		        .append("\n\tMean: ").append(mean[i])
		        .append("\n\tVariance: ").append(variance[i])
		        .append("\n\tStandard deviation: ").append(stddev[i])
		        .append("\n\tMedian: ").append(median[i])
		        .append("\n\tFirst Quartile: ").append(firstQuartile[i])
		            .append(", Third Quartile: ").append(thirdQuartile[i]);
		}
		
		return res.toString();
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
