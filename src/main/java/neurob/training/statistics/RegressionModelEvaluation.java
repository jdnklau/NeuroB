package neurob.training.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.deeplearning4j.eval.RegressionEvaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.statistics.interfaces.ModelEvaluation;

public class RegressionModelEvaluation extends ModelEvaluation<RegressionEvaluation> {
	protected BufferedWriter epochCSV;
	protected int numColumns;
	private double bestR2;
	
	private static final Logger log = LoggerFactory.getLogger(RegressionModelEvaluation.class); 

	public RegressionModelEvaluation(NeuroBNet model) {
		super(model);
		numColumns = nbn.getOutputSize();
		bestR2 = -1; // value between 0 and 1, so -1 will always be outperformed
	}

	@Override
	protected void setupCSV(Path csv) throws IOException {
		epochCSV = Files.newBufferedWriter(csv);
		
		// setting up csv header
		List<String> columns = new ArrayList<>();
		columns.add("Epoch");
		// training set stats
		columns.addAll(setup_header("Training"));
		columns.addAll(setup_header("Test"));
		
		epochCSV.write(String.join(",", columns));
		epochCSV.newLine();
		epochCSV.flush();
	}

	private List<String> setup_header(String caption) {
		List<String> columns = new ArrayList<>();
		
		for(int c=0; c<numColumns; c++){
			columns.add(caption+" MSE #"+c);
			columns.add(caption+" MAE #"+c);
			columns.add(caption+" RMSE #"+c);
			columns.add(caption+" RSE #"+c);
			columns.add(caption+" R2 #"+c);
		}
		
		return columns;
	}
	
	@Override
	protected void writeEvaluationToCSV(RegressionEvaluation trainEval, RegressionEvaluation testEval)
			throws IOException {
		// set up line of csv
		List<String> columns = new ArrayList<>();
		columns.add(Integer.toString(epochsSeen));
		columns.addAll(partialCSVEntries(trainEval));
		columns.addAll(partialCSVEntries(testEval));
		
		epochCSV.write(String.join(",", columns));
		epochCSV.newLine();
		epochCSV.flush();
	}
	
	@Override
	protected void compareWithBestEpoch(RegressionEvaluation testEval) {
		if(!performsBetterThan(testEval)){
			// NOTE: is already calculated in performsBetterThan(testEval)
			// but I see no way to store/return/access the value
			// in a way that makes sense.
			double r2mean = r2mean(testEval);
			// found new best epoch
			bestEpochSeen = epochsSeen;
			log.info("\tImproved on epoch {}: Mean correlation coefficient (R2) {}->{}",
					epochsSeen, bestR2, r2mean);
			bestR2 = r2mean;
		}
	}
	
	protected List<String> partialCSVEntries(RegressionEvaluation eval){
		List<Double> entries = new ArrayList<>();
		
		for(int c=0; c<numColumns; c++){
			entries.add(eval.meanSquaredError(c));
			entries.add(eval.meanAbsoluteError(c));
			entries.add(eval.rootMeanSquaredError(c));
			entries.add(eval.relativeSquaredError(c));
			entries.add(eval.correlationR2(c));
		}
		
		return entries.stream().map(d->d.toString()).collect(Collectors.toList());
	}

	@Override
	public RegressionEvaluation evaluateModel(Path testSet) throws IOException, InterruptedException {
		RegressionEvaluation eval = new RegressionEvaluation(numColumns);
		return evaluateModel(testSet, eval);
	}
	
	@Override
	public RegressionEvaluation evaluateModel(Path testSet, String caption) throws IOException, InterruptedException {
		RegressionEvaluation eval = evaluateModel(testSet);
		for(int c=0; c<numColumns; c++){
			log.info("\t{}, column #{} --- MSE: {}; MAE: {}; RMSE: {}; RSE: {}; R2: {}",
				caption, c,
				eval.meanSquaredError(c),
				eval.meanAbsoluteError(c),
				eval.rootMeanSquaredError(c),
				eval.relativeSquaredError(c),
				eval.correlationR2(c));
		}
		return eval;
	}

	@Override
	public RegressionEvaluation evaluateAfterTraining(Path testSet) throws NeuroBException {
		RegressionEvaluation eval;
		try {
			eval = evaluateModel(testSet);
		} catch (IOException | InterruptedException e) {
			throw new NeuroBException("Could not evaluate test set", e);
		}
		
		// log values for each column
		for(int c=0; c<numColumns; c++){
			log.info("Regression performances for column #{}:", c);
			log.info("\tMean squared error: {}", eval.meanSquaredError(c));
			log.info("\tMean absolute error: {}", eval.meanAbsoluteError(c));
			log.info("\tRoot mean squared error: {}", eval.rootMeanSquaredError(c));
			log.info("\tRelative squared error: {}", eval.relativeSquaredError(c));
			log.info("\tCorrelation coefficient (R2): {}", eval.correlationR2(c));
		}
		
		return eval;
	}

	@Override
	protected boolean performsBetterThan(RegressionEvaluation first, RegressionEvaluation second) {
		return r2mean(first) > r2mean(second);
	}
	
	private double r2mean(RegressionEvaluation eval){
		double r2sum = 0;
		for(int i=0; i<numColumns; i++){
			r2sum = eval.correlationR2(i);
		}
		return r2sum/numColumns;
	}

}
