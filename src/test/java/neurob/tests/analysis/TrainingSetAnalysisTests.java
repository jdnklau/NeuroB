package neurob.tests.analysis;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import neurob.training.TrainingSetAnalyser;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;

public class TrainingSetAnalysisTests {

	@Test
	public void AnalysisFileTest() throws IOException {
		Path testFile = Paths.get("src/test/resources/training_analysis_testset.csv");
		Path analysisFile = Paths.get("src/test/resources/analysis.txt");
		LabelGenerator labelgen = new SolverSelectionGenerator();
		TrainingAnalysisData data = TrainingSetAnalyser.analyseTrainingCSV(testFile, labelgen);
		
		Files.deleteIfExists(analysisFile);
		TrainingSetAnalyser.writeTrainingAnalysis(data, analysisFile.getParent());
		
		assertTrue("Analysis file not correctly created.", Files.exists(analysisFile));
		Files.deleteIfExists(analysisFile);
	}

}
