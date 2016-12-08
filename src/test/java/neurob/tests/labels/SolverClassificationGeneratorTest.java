package neurob.tests.labels;

import static org.junit.Assert.*;

import org.junit.Test;

import neurob.training.generators.labelling.SolverClassificationGenerator;

public class SolverClassificationGeneratorTest {

	@Test
	public void useAllSolversDimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(true, true, true);
		expected = 3;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with ProB, KodKod, ProBZ3", expected, actual);
	}
	
	@Test
	public void useProBKodKodDimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(true, true, false);
		expected = 2;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with ProB, KodKod", expected, actual);
	}
	
	@Test
	public void useProBProBZ3DimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(true, false, true);
		expected = 2;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with ProB, ProBZ3", expected, actual);
	}
	
	@Test
	public void useKodKodProBZ3DimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(false, true, true);
		expected = 2;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with KodKod, ProBZ3", expected, actual);
	}
	
	@Test
	public void useProBDimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(true, false, false);
		expected = 1;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with ProB", expected, actual);
	}
	
	@Test
	public void useKodKodDimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(false, true, false);
		expected = 1;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with KodKod", expected, actual);
	}
	
	@Test
	public void useProBZ3DimensionSizeTest() {
		SolverClassificationGenerator lg; 
		int actual, expected;
		
		// test for all
		lg = new SolverClassificationGenerator(false, false, true);
		expected = 1;
		// label dimension
		actual = lg.getLabelDimension();
		assertEquals("Dimensions do not match of labelling vector with ProBZ3", expected, actual);
	}

}
