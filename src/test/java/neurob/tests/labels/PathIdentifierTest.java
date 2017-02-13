package neurob.tests.labels;

import static org.junit.Assert.*;

import neurob.core.util.SolverType;
import neurob.training.generators.labelling.*;

import org.junit.Test;

public class PathIdentifierTest {

	@Test
	public void SolverClassificationProBTest() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(SolverType.PROB);
		
		String expected = "SolverClassificationGenerator_ProB";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationKodKodTest() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(SolverType.KODKOD);
		
		String expected = "SolverClassificationGenerator_KodKod";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationZ3Test() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(SolverType.SMT_SUPPORTED_INTERPRETER);
		
		String expected = "SolverClassificationGenerator_SMT_SUPPORTED_INTERPRETER";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

}
