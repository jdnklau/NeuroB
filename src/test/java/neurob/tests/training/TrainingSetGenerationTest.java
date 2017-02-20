package neurob.tests.training;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Test;

import neurob.core.features.PredicateFeatures;
import neurob.core.util.SolverType;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.analysis.ClassificationAnalysis;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;

public class TrainingSetGenerationTest {
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	private final Path formulaeGenNBTrain = Paths.get("src/test/resources/training/nbtrain/formulae_generation.nbtrain");
	
	/**
	 * Test if the collector runs properly or exits with an error
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws BException
	 */
	@Test
	public void trainingSetAnalysisForNBTrain() throws IOException{
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.PROB);
		TrainingSetGenerator tsg = new TrainingSetGenerator(new PredicateFeatures(), lg);
		
		Files.deleteIfExists(formulaeGenNBTrain);
		
		// generate
		tsg.generateTrainingDataFromFile(formulaeGenTestFile, formulaeGenNBTrain);
		
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		
		// analyse
		ClassificationAnalysis analysis = tsa.analyseNBTrainSet(Paths.get("src/test/resources/training/nbtrain/"), lg);

		assertEquals("File counter does not match", 1, analysis.getFilesCount());
		assertEquals("Empty files counter does not match", 0, analysis.getEmptyFilesCount());
		
		ArrayList<Integer> trueLabels = analysis.getTrueLabelCounters();
		assertEquals("Class 0 counter does not match", 0, trueLabels.get(0).intValue());
		assertEquals("Class 1 counter does not match", 98, trueLabels.get(1).intValue());
		
		Files.deleteIfExists(formulaeGenNBTrain);
	}

	@Test
	public void trainingSetAnalysisForCSV() throws IOException{
		LabelGenerator lg = new SolverSelectionGenerator();		
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		
		// analyse
		ClassificationAnalysis analysis = tsa.analyseTrainingCSV(Paths.get("src/test/resources/training_analysis_testset.csv"), lg);
		
		ArrayList<Integer> trueLabels = analysis.getTrueLabelCounters();
		assertEquals("Class 0 counter does not match", 3, trueLabels.get(0).intValue());
		assertEquals("Class 1 counter does not match", 989, trueLabels.get(1).intValue());
		assertEquals("Class 2 counter does not match", 3, trueLabels.get(2).intValue());
		assertEquals("Class 3 counter does not match", 3, trueLabels.get(3).intValue());
		
		int samples = 0;
		for(int i = 0; i<4; i++){
			samples += trueLabels.get(i);
		}
		
		assertEquals("Number of samples seen in total does not match", 998, samples);
	}
}
