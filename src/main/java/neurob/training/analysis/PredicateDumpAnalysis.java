package neurob.training.analysis;

import java.util.Arrays;
import java.util.Random;

import neurob.core.util.SolverType;
import neurob.training.generators.labelling.PredicateDumpLabelGenerator;

public class PredicateDumpAnalysis extends RegressionAnalysis {
	// trimming utility
	private final SolverType trimSolver; // trimming happens wrt this solver
	private final int trimIdx; // index of solver in used arrays
	private double trimChanceDecidable; // trim chance for samples decidable by trimSolver
	private double trimChanceUndecidable; // trim chance for samples undecidable by trimSolver
	private final Random rng = new Random(123);
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

	public PredicateDumpAnalysis(){
		this(null);
	}

	/**
	 * Sets the analysis up to treat the given solver specially.
	 * <p>
	 * The solver will e.g. be used for trimming the analysed data set with respect to it.
	 * If {@code solver==null} no solver is treated differently and trimming does not happen
	 * (e.g. {@link #canSampleBeTrimmed(double[])} will always return {@code null}).
	 * @param solver Solver to be treated specially
	 */
	public PredicateDumpAnalysis(SolverType solver) {
		super(solversAccountedFor);
		samplesSeen=0;

		decidabilityDist = new double[solversAccountedFor];
		decidabilityMatrix = new int[solversAccountedFor][solversAccountedFor];

		negSamples = new long[]{0L,0L,0L,0L};

		// save solver to use for trimming
		trimSolver = solver;
		trimIdx = PredicateDumpLabelGenerator.getSolverIndex(trimSolver);
		trimChanceDecidable = 1; // this corresponds to always trimming; overwritten in #evaluateAllSamples
		trimChanceUndecidable = 1; // this corresponds to always trimming; overwritten in #evaluateAllSamples
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
		res.append("Samples seen: ").append(samplesSeen).append("\n");

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
					.append(", Third Quartile: ").append(thirdQuartile[i])
				.append("\n");
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

		// calculate trimChanceDecidable for samples decidable by trimSolver
		if(trimSolver != null){
			double trimDecidability = decidabilityDist[trimIdx];
			double smallerClassDist = Math.min(trimDecidability, 1-trimDecidability);
			trimChanceDecidable = 1-smallerClassDist/trimDecidability;
			trimChanceUndecidable = 1-smallerClassDist/(1-trimDecidability);
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
		if(trimSolver==null){
			// NOTE: In regression we do not need trimming
			// => trim everything, so no new files are generated
			return true;
		}

		// For solver to trim for, decide if example is decided or not
		boolean isDecidable = trainingLabels[trimIdx] >= 0;

		// take chances for trimming
		double chance = rng.nextDouble();
		double trimChance = (isDecidable) ? trimChanceDecidable : trimChanceUndecidable;

		// return trimmability
		return chance <= trimChance;
	}

}
