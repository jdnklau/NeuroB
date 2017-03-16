package neurob.tests.features;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.datavec.api.util.ClassPathResource;
import org.junit.Test;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.features.TheoryFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.PredicateCollector;

public class TheoryFeaturesTest {
	private String testpred;
	private File resource;
	private Api api;
	
	@Inject
	public TheoryFeaturesTest() throws FileNotFoundException {
		testpred = "x : NAT & y > x";
		resource = new ClassPathResource("features_check.mch").getFile();
		
		api = Main.getInjector().getInstance(Api.class); 
	}

	@Test
	public void getFeatureStringTest() throws IOException, NeuroBException, ModelTranslationError{
		StateSpace ss;
		
		ss = api.b_load(resource.toString());
		
		PredicateCollector pc = new PredicateCollector(ss);
		List<String> invariants = pc.getInvariants();

		ss.kill();
		
		String pred = String.join("&", invariants);
		
		TheoryFeatures f = new TheoryFeatures();
		
		f.addData(pred);
		
		String actual = f.getFeatureStrings().get(0);
		String expected = "0,5,1,1,9,0,0,0,6,0,0,6,4,2,0,1,0";
		
		assertEquals("Generated features do not match",expected, actual);
	}

	@Test
	public void getFeatureArray() throws IOException, ModelTranslationError, NeuroBException{
		StateSpace ss;
		
		ss = api.b_load(resource.toString());
		
		PredicateCollector pc = new PredicateCollector(ss);
		List<String> invariants = pc.getInvariants();

		ss.kill();
		
		String pred = String.join("&", invariants);
		
		TheoryFeatures f = new TheoryFeatures();
		
		double[] actual = f.generateFeatureArray(pred);
		double[] expected = new double[]{0.,5.,1.,1.,9.,0.,0.,0.,6.,0.,0.,6.,4.,2.,0.,1.,0.}; 
									//   0, 5, 1, 1, 9, 0, 0, 0, 6, 0, 0, 6, 4, 2, 0, 1, 0";
		
		assertArrayEquals("Generated features do not match",expected, actual, 0.0001);
	}
	
	/**
	 * This test is intended to check that the feature dimension returned matches the size of the output vector
	 * @throws NeuroBException 
	 */
	@Test
	public void featureDimensionTest() throws NeuroBException{
		TheoryFeatures f = new TheoryFeatures();
		
		String res = f.generateFeatureString(testpred);
		
		int actual = res.split(",").length;
		int expected = f.getFeatureDimension();
		
		assertEquals("Feature dimensions do not match for String representation", expected, actual);
		
		actual = f.generateFeatureArray(testpred).length;
		assertEquals("Feature dimensions do not match for double[] representation", expected, actual);
		
		actual = f.generateFeatureNDArray(testpred).length();
		assertEquals("Feature dimensions do not match for INDArray representation", expected, actual);
	}

}
