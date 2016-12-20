package neurob.tests.labels;

import static org.junit.Assert.*;
import neurob.training.generators.labelling.*;

import org.junit.Test;

public class PathIdentifierTest {

	@Test
	public void SolverClassificationProBKodKodZ3Test() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(true, true, true);
		
		String expected = "SolverClassificationGenerator_ProB_KodKod_Z3";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}
	
	@Test
	public void SolverClassificationProBKodKodTest() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(true, true, false);
		
		String expected = "SolverClassificationGenerator_ProB_KodKod";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationProBZ3Test() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(true, false, true);
		
		String expected = "SolverClassificationGenerator_ProB_Z3";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationProBTest() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(true, false, false);
		
		String expected = "SolverClassificationGenerator_ProB";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationKodKodZ3Test() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(false, true, true);
		
		String expected = "SolverClassificationGenerator_KodKod_Z3";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationKodKodTest() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(false, true, false);
		
		String expected = "SolverClassificationGenerator_KodKod";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

	@Test
	public void SolverClassificationZ3Test() {
		SolverClassificationGenerator gen = new SolverClassificationGenerator(false, false, true);
		
		String expected = "SolverClassificationGenerator_Z3";
		String actual = gen.getDataPathIdentifier();
		
		assertEquals("Path identifying strings do not match.", expected, actual);
	}

}
