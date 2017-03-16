package neurob.tests.training.pdumps;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import neurob.core.features.TheoryFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.PredicateDumpTranslator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;
import neurob.training.generators.labelling.SolverTimerGenerator;

public class PredicateDumperTranslationTest {
	private final String predicateDump = "-1,-1,2525884156:x:NATURAL & y:NATURAL & z:INTEGER & y<x & z<20 <=> x<y";
	
	private final Path schleusenMch = Paths.get("src/test/resources/Schleusen.mch");
	private final Path schleusenPdump = Paths.get("src/test/resources/Schleusen.pdump");
	
	@Test
	public void SolverClassificationPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverClassificationGenerator(SolverType.PROB);
		FeatureGenerator fg = new TheoryFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,0";
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver classification (ProB) does not match", expected, actual);
		

		pds = new SolverClassificationGenerator(SolverType.Z3);
		
		expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,1";
		actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver classification (ProB+Z3) does not match", expected, actual);
	}
	
	@Test
	public void SolverSelectionPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverSelectionGenerator();
		FeatureGenerator fg = new TheoryFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,3";
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver selection does not match", expected, actual);
	}
	

	@Test
	public void SolverTimerPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverTimerGenerator(3);
		FeatureGenerator fg = new TheoryFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,-1.0,-1.0," + 2525884156L/1e6;
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver selection does not match", expected, actual);
	}
	
	@Test
	public void TranslateToTheoryFeaturesWithParsedFileTest() throws NeuroBException, IOException{
		PredicateDumpTranslator pdt = new SolverClassificationGenerator(SolverType.PROB);
		FeatureGenerator fg = new TheoryFeatures(schleusenMch);
		
		List<String> lines = Files.lines(schleusenPdump).collect(Collectors.toList());
		
		String expected = "0,2,1,0,3,0,0,0,2,1,0,8,0,0,8,1,0,1";
		String actual;
		try{
			actual = pdt.translateToCSVDataString(fg, lines.get(0));
			assertEquals("Feature representation does not match", expected, actual);
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
		PredicateDumpTranslator pdt = new SolverClassificationGenerator(SolverType.PROB);
		FeatureGenerator fg = new TheoryFeatures();
		
		List<String> lines = Files.lines(schleusenPdump).collect(Collectors.toList());
		
		try{
			pdt.translateToCSVDataString(fg, lines.get(0));
			assertTrue("Could unexpectedly parse the predicate given.", false);
		} catch(NeuroBException e){
			// nothing
		}
	}
}
