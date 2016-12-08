package neurob.tests.features;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.datavec.api.util.ClassPathResource;
import org.junit.Test;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.PredicateFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.PredicateCollector;

public class PredicateFeaturesTest {
	private String testpred;
	private File resource;
	private Api api;
	
	@Inject
	public PredicateFeaturesTest() throws FileNotFoundException {
		testpred = "x : NAT & y > x";
		resource = new ClassPathResource("features_check.mch").getFile();
		
		api = Main.getInjector().getInstance(Api.class); 
	}

	@Test
	public void getFeatureStringTest() throws IOException, BException, NeuroBException{
		AbstractElement mainComp;
		StateSpace ss;
		
		ss = api.b_load(resource.toString());
		
		mainComp = ss.getMainComponent();
		ss.kill();
		
		PredicateCollector pc = new PredicateCollector(mainComp);
		List<String> invariants = pc.getInvariants();
		
		String pred = String.join("&", invariants);
		
		PredicateFeatures f = new PredicateFeatures();
		
		f.addData(pred);
		
		String actual = f.getFeatureStrings().get(0);
		String expected = "0,5,1,1,9,0,0,0,6,0,0,6,4,2,0,1,0";
		
		assertEquals("Generated features do not match",expected, actual);
	}

	@Test
	public void getFeatureArray() throws IOException, BException, NeuroBException{
		AbstractElement mainComp;
		StateSpace ss;
		
		ss = api.b_load(resource.toString());
		
		mainComp = ss.getMainComponent();
		ss.kill();
		
		PredicateCollector pc = new PredicateCollector(mainComp);
		List<String> invariants = pc.getInvariants();
		
		String pred = String.join("&", invariants);
		
		PredicateFeatures f = new PredicateFeatures();
		
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
		PredicateFeatures f = new PredicateFeatures();
		
		String res = f.generateFeatureString(testpred);
		
		int actual = res.split(",").length;
		int expected = f.getfeatureDimension();
		
		assertEquals("Feature dimensions do not match for String representation", expected, actual);
		
		actual = f.generateFeatureArray(testpred).length;
		assertEquals("Feature dimensions do not match for double[] representation", expected, actual);
		
		actual = f.generateFeatureNDArray(testpred).length();
		assertEquals("Feature dimensions do not match for INDArray representation", expected, actual);
	}

}
