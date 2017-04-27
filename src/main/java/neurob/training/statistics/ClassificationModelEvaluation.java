package neurob.training.statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.eval.ConfusionMatrix;
import org.deeplearning4j.eval.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.statistics.interfaces.ModelEvaluation;

public class ClassificationModelEvaluation extends ModelEvaluation<Evaluation> {
	protected BufferedWriter epochCSV;
	private static final Logger log = LoggerFactory.getLogger(ClassificationModelEvaluation.class);

	public ClassificationModelEvaluation(NeuroBNet model) {
		super(model);
	}

	@Override
	protected void setupCSV(Path csv) throws IOException {
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
	protected void writeEvaluationToCSV(Evaluation trainEval, Evaluation testEval) throws IOException {
		// set up line of csv
		List<String> columns = new ArrayList<>();
		columns.add(Integer.toString(epochsSeen));
		columns.addAll(partialCSVEntries(trainEval));
		columns.addAll(partialCSVEntries(testEval));
		
		epochCSV.write(String.join(",", columns));
		epochCSV.newLine();
		epochCSV.flush();
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
	protected void compareWithBestEpoch(Evaluation testEval) {
		if(!performsBetterThan(testEval)){
			// found new best epoch
			bestEpochSeen = epochsSeen;
			log.info(
					"\tImproved on epoch {}: Accuracy {}->{}, F1 Score {}->{}",
					epochsSeen,
					bestEvaluation.accuracy(), testEval.accuracy(),
					bestEvaluation.f1(), testEval.f1());
		}
	}

	@Override
	public Evaluation evaluateModel(Path testSet) throws IOException, InterruptedException {
		Evaluation eval = new Evaluation(nbn.getClassificationSize());
		return evaluateModel(testSet, eval);
	}
	
	@Override
	public Evaluation evaluateModel(Path testSet, String caption) throws IOException, InterruptedException {
		Evaluation testEval = evaluateModel(testSet);
		log.info("\t{} stats - Accuracy: {}; Precision: {}; Recall: {}; F1 score: {}",
				caption,
				testEval.accuracy(),
				testEval.precision(),
				testEval.recall(),
				testEval.f1());
		return testEval;
	}

	@Override
	public Evaluation evaluateAfterTraining(Path testSet) throws NeuroBException {
		Evaluation eval;
		try {
			eval = evaluateModel(testSet);
		} catch (IOException | InterruptedException e) {
			throw new NeuroBException("Could not evaluate test set", e);
		}
		
		// log evaluation results
		log.info("Accuracy: {}", eval.accuracy());
		log.info("Precision: {}", eval.precision());
		log.info("Recall: {}", eval.recall());
		log.info("F1 score: {}", eval.f1());

		// log confusion matrix
		ConfusionMatrix<Integer> matrix = eval.getConfusionMatrix();
		log.info("Confusion Matrix:");
		for(int i=0; i<nbn.getClassificationSize(); i++){
			for(int j=0; j<nbn.getClassificationSize(); j++){
				log.info("\tClass {} predicted as {} a total of {} times", i, j,
						matrix.getCount(i, j));
			}
		}
		
		return eval;
	}

	@Override
	protected boolean performsBetterThan(Evaluation first, Evaluation second) {
		return second.f1()>first.f1() || second.accuracy()>first.accuracy();
	}

}
