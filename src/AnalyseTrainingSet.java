import java.nio.file.Paths;

import neurob.training.TrainingSetAnalyser;

public class AnalyseTrainingSet {

	public static void main(String[] args) {
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(Paths.get("prob_examples/training_data"));
		System.out.println(tsa.getStatistics());

	}

}
