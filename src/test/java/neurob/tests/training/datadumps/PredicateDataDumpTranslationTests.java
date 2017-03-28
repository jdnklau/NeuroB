package neurob.tests.training.datadumps;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import neurob.core.features.TheoryFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.DataDumpTranslator;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;
import neurob.training.generators.labelling.SolverTimerGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateDataDumpTranslationTests {
	private final String predicateDump = "-1,-1,2525884156:x:NATURAL & y:NATURAL & z:INTEGER & y<x & z<20 <=> x<y";
	private final Path schleusenMch = Paths.get("src/test/resources/Schleusen.mch");
	private final Path schleusenPdump = Paths.get("src/test/resources/Schleusen.pdump");
	

	@Test
	public void SolverClassificationProBTranslationTest() throws NeuroBException {
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.PROB);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		TrainingData dd = ddt.translateDataDumpEntry(predicateDump);
		
		double[] expected = {0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,0};
		double[] actual = dd.getTrainingVector();
		
		assertArrayEquals("Predicate dump translation for solver classification (ProB) does not match", expected, actual, 0.01);
		
	}
	
	@Test
	public void SolverClassificationZ3TranslationTest() throws NeuroBException {
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.Z3);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		double[] expected = {0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,1};
		double[] actual = ddt.translateDataDumpEntry(predicateDump).getTrainingVector();

		assertArrayEquals("Predicate dump translation for solver classification (Z3) does not match", expected, actual, 0.01);
	}
	
	@Test
	public void SolverSelectionTranslationTest() throws NeuroBException {
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverSelectionGenerator();
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		double[] expected = {0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,3};
		double[] actual = ddt.translateDataDumpEntry(predicateDump).getTrainingVector();

		assertArrayEquals("Predicate dump translation for solver selection does not match", expected, actual, 0.01);
	}
	
	@Test
	public void SolverTimerTranslationTest() throws NeuroBException {
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverTimerGenerator(3);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		double[] expected = {0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,-1.0,-1.0,2525884156L/1e6};
		double[] actual = ddt.translateDataDumpEntry(predicateDump).getTrainingVector();

		assertArrayEquals("Predicate dump translation for solver timer prediction does not match", expected, actual, 0.01);
	}
	

	
	@Test
	public void TranslateToTheoryFeaturesWithParsedFileTest() throws NeuroBException, IOException{
		FeatureGenerator fg = new TheoryFeatures(schleusenMch);
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.PROB);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		List<String> lines = Files.lines(schleusenPdump).collect(Collectors.toList());
		
		double[] expected = {0,2,1,0,3,0,0,0,2,1,0,8,0,0,8,1,0,1};
		double[] actual;
		try{
			actual = ddt.translateDataDumpEntry(lines.get(1)).getTrainingVector();
			assertArrayEquals("Feature representation does not match", expected, actual, 0.01);
		} catch(NeuroBException e){
			assertTrue(e.getMessage(), false);
		}
	}
	
	/**
	 * This test is a sanity check of {@link #TranslateToTheoryFeaturesWithParsedFileTest()}.
	 * <p>
	 * It checks whether parsing the file or not leads to different results.
	 * @throws NeuroBException
	 * @throws IOException
	 */
	@Test
	public void TranslateToTheoryFeaturesWithoutParsedFileTest() throws NeuroBException, IOException{
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.PROB);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		List<String> lines = Files.lines(schleusenPdump).collect(Collectors.toList());
		
		try{
			ddt.translateDataDumpEntry(lines.get(1)).getTrainingVector();
			assertTrue("Could unexpectedly parse the predicate given.", false);
		} catch(NeuroBException e){
			// desired behaviour
		}
	}
	
	@Test
	public void DirectoryHierarchyTest() throws IOException{
		FeatureGenerator fg = new TheoryFeatures();
		LabelGenerator lg = new SolverClassificationGenerator(SolverType.PROB);
		DataDumpTranslator ddt = new DataDumpTranslator(fg.getTrainingDataGenerator(lg));
		
		final Path targetDir = Paths.get("src/test/resources/tmp/pdumptranslation");
		final Path targetFile = targetDir.resolve("src/test/resources/Schleusen.nbtrain");
		
		Files.deleteIfExists(targetFile);
		
		ddt.translateDumpFile(schleusenPdump, targetDir);
		
		assertTrue("File was not properly created.", Files.exists(targetFile));
		Files.deleteIfExists(targetFile);
		
	}

}
