package neurob.training.analysis;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

import neurob.core.util.SolverType;
import neurob.training.TrainingSetAnalyser;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.PredicateDumpLabelGenerator;
import neurob.training.generators.util.DumpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateDumpAnalysis extends RegressionAnalysis {
	// trimming utility
	private final SolverType trimSolver; // trimming happens wrt this solver
	private final int trimIdx; // index of solver in used arrays
	private LabelGenerator labelGenerator; // for trimming, to trim as data would have been translated
	private TrainingAnalysisData wrappedData;
	private boolean wrappedAnalysis = false;
	private double trimChanceDecidable; // trim chance for samples decidable by trimSolver
	private double trimChanceUndecidable; // trim chance for samples undecidable by trimSolver
	private final Random rng = new Random(123);
	// data over solvers
	private long[] negSamples;
	private final static int solversAccountedFor = PredicateDumpLabelGenerator.solversAccountedFor;
	private double[] decidabilityDist; // Distribution of decidability of samples between solvers
	// logger
	private final static Logger log = LoggerFactory.getLogger(PredicateDumpAnalysis.class);


	/**
	 * Count of samples pairwise decidable by two solvers.
	 * Calculated as upper triangular matrix
	 * <br>Indices through each dimension are: 0 ProB, 1 KodKod, 2 Z3, 3 ProB+Z3
	 */
	private int[][] decidabilityMatrix;
	private double totalPredicateLength=0;
	private int minPredicateLength=Integer.MAX_VALUE;
	private int maxPredicateLength=Integer.MIN_VALUE;

	public PredicateDumpAnalysis(){
		this((SolverType) null);
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

	public PredicateDumpAnalysis(LabelGenerator lg) {
		this((SolverType) null);
		this.labelGenerator = lg;

		this.wrappedData = TrainingSetAnalyser.getAnalysisTypeByProblem(lg);
		this.wrappedAnalysis = true;
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
		res.append("Predicate metrics:\n");
		res.append("\tminimum length: ").append(minPredicateLength)
				.append("; maximum length: ").append(maxPredicateLength);
		res.append("\n\taverage predicate length: ").append(totalPredicateLength/samplesSeen)
				.append("\n");

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

		if(wrappedAnalysis){
			res.append(wrappedData.getStatistics());
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

		if(wrappedAnalysis){
			wrappedData.evaluateAllSamples();
		}

		return this;
	}

	/**
	 * Returns an array containing a percentage of how many samples were decidable by each solver.
	 * This should only be called <b>after</b> {@link #evaluateAllSamples()} was called.
	 * <p>
	 * The indices map to the solvers like given by {@link PredicateDumpLabelGenerator#solverOrder}.
	 * Similar, {@link PredicateDumpLabelGenerator#getSolverIndex(SolverType)} returns the matching
	 * index to a given solver.
	 * @return Array of percentages of how many samples each solver could decide.
	 */
	public double[] getDecidabilityDist() {
		return decidabilityDist;
	}

	/**
	 * Returns a matrix containing the pair wise decidability of two solvers. Each entry represents
	 * the number of samples decidable by both.
	 * <p>
	 * The indices map to the solvers like given by {@link PredicateDumpLabelGenerator#solverOrder}.
	 * Similar, {@link PredicateDumpLabelGenerator#getSolverIndex(SolverType)} returns the matching
	 * index to a given solver.
	 * @return Matrix containing number of samples decidable pair wise by two solvers
	 */
	public int[][] getDecidabilityMatrix() {
		return decidabilityMatrix;
	}

	private double[] translateLabels(double[] labels, LabelGenerator lg){
		String labelStr =
				String.join(",",
						Arrays.stream(labels).mapToObj(Double::toString)
						.collect(Collectors.toList()));

		return labelGenerator.translateLabelling(
				new DumpData(labelStr+":TRUE"));
	}

	@Override
	public void analyseSample(double[] featureStatistics, double[] labels) {
		if(wrappedAnalysis){
			wrappedData.analyseSample(
					new double[]{},
					translateLabels(labels, labelGenerator));
		}

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
		// Split predicate
		int idx = sampleString.indexOf(":");
		String labelString = sampleString.substring(0,idx);
		String pred = sampleString.substring(idx+1);
		// Collect predicate metrics
		int predlen = pred.length();
		totalPredicateLength += predlen;
		if(predlen < minPredicateLength)
			minPredicateLength = predlen;
		else if(predlen > maxPredicateLength)
			maxPredicateLength = predlen;

		double[] labels = Arrays.stream(labelString.split(","))
				.mapToDouble(Double::parseDouble).toArray();
		analyseSample(null, labels);
	}

	@Override
	public boolean canSampleBeTrimmed(double[] trainingLabels) {
		if(wrappedAnalysis){
			return wrappedData.canSampleBeTrimmed(translateLabels(trainingLabels,labelGenerator));
		}

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
