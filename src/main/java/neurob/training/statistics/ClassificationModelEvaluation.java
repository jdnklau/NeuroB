package neurob.training.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.eval.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.statistics.interfaces.ModelEvaluation;

public class ClassificationModelEvaluation extends ModelEvaluation<Evaluation> {
	protected BufferedWriter epochCSV;
	private static final Logger log = LoggerFactory.getLogger(ClassificationModelEvaluation.class);

	public ClassificationModelEvaluation(NeuroBNet model, Path csvFile) throws IOException {
		super(model, csvFile);
	}

	@Override
	protected void setup(Path csv) throws IOException {
		epochCSV = Files.newBufferedWriter(csv);
		
		// setting up csv header
		List<String> columns = new ArrayList<>();
		columns.add("Epoch");
		// training set stats
		columns.add("Training Accuracy");
		columns.add("Training Precision");
		columns.add("Training Recall");
		columns.add("Training F1 Score");
		// test set stats
		columns.add("Test Accuracy");
		columns.add("Test Precision");
		columns.add("Test Recall");
		columns.add("Test F1 Score");
		
		epochCSV.write(String.join(",", columns));
		epochCSV.newLine();
		epochCSV.flush();
	}

	@Override
	public void evaluateAfterEpoch(Path trainingSet, Path testSet) throws NeuroBException {
		epochsSeen++; // new epoch seen
		
		Evaluation trainEval;
		Evaluation testEval;
		try {
			trainEval = evaluateModel(trainingSet);
			testEval = evaluateModel(testSet);
		} catch (IOException | InterruptedException e) {
			throw new NeuroBException("Could not evaluate training or test set.", e);
		}
		
		// log results
		logEvaluation("Training", trainEval);
		logEvaluation("Testing", testEval);
		
		// set up line of csv
		List<String> columns = new ArrayList<>();
		columns.add(Integer.toString(epochsSeen));
		columns.addAll(partialCSVEntries(trainEval));
		columns.addAll(partialCSVEntries(testEval));
		
		try {
			epochCSV.write(String.join(",", columns));
			epochCSV.flush();
		} catch (IOException e) {
			throw new NeuroBException("Unable to write statistics for epoch "+epochsSeen+" to csv", e);
		}
		
		// evaluate best epoch
		// TODO: actually implement a metric to check if new evaluation performed better.
		bestEpochSeen = epochsSeen; 
		log.info("\tBest epoch thus far: #{}", bestEpochSeen);
	}
	
	private void logEvaluation(String caption, Evaluation testEval) {
		log.info("\t{} stats - Accuracy: {}; Precision: {}; Recall: {}; F1 score: {}",
				caption,
				testEval.accuracy(),
				testEval.precision(),
				testEval.recall(),
				testEval.f1());
	}

	protected List<String> partialCSVEntries(Evaluation eval){
		List<String> entries = new ArrayList<>();
		// test set stats
		entries.add(Double.toString(eval.accuracy()));
		entries.add(Double.toString(eval.precision()));
		entries.add(Double.toString(eval.recall()));
		entries.add(Double.toString(eval.f1()));
		
		return entries;
	}

	@Override
	public Evaluation evaluateModel(Path testSet) throws IOException, InterruptedException {
		Evaluation eval = new Evaluation(nbn.getClassificationSize());
		return evaluateModel(testSet, eval);
	}

}
