package neurob.tests.training;

import static org.junit.Assert.*;

import org.junit.Test;

import neurob.core.features.PredicateFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.PredicateDumpTranslator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;
import neurob.training.generators.labelling.SolverTimerGenerator;

public class PredicateDumperTranslationTest {
	private final String predicateDump = "-1,-1,2525884156:x:NATURAL & y:NATURAL & z:INTEGER & y<x & z<20 <=> x<y";
	
	@Test
	public void SolverClassificationPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverClassificationGenerator(SolverType.PROB);
		FeatureGenerator fg = new PredicateFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,0";
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver classification (ProB) does not match", expected, actual);
		

		pds = new SolverClassificationGenerator(SolverType.SMT_SUPPORTED_INTERPRETER);
		
		expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,1";
		actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver classification (ProB+Z3) does not match", expected, actual);
	}
	
	@Test
	public void SolverSelectionPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverSelectionGenerator();
		FeatureGenerator fg = new PredicateFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,3";
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver selection does not match", expected, actual);
	}
	

	@Test
	public void SolverTimerPredicateDumpTranslationTest() throws NeuroBException{
		PredicateDumpTranslator pds = new SolverTimerGenerator(3);
		FeatureGenerator fg = new PredicateFeatures();

		
		String expected = "0,3,0,0,4,0,0,0,3,0,0,3,0,3,0,0,1,-1.0,-1.0," + 2525884156L/1e6;
		String actual = pds.translateToCSVDataString(fg, predicateDump);
		
		assertEquals("Predicate dump translation for solver selection does not match", expected, actual);
	}

}
