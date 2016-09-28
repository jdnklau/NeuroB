package neurob.tests.features;

import static org.junit.Assert.*;

import java.io.File;

import org.datavec.api.util.ClassPathResource;
import org.junit.Test;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;

public class FeatureDataTest {
	
	/** Helper functions **/
	private FeatureData getFeatureData() throws Exception {
		BParser p = new BParser();
		File resource = new ClassPathResource("features_check.mch").getFile();
		Start ast = p.parseFile(resource, false);
		
		FeatureCollector fc = new FeatureCollector();
		ast.apply(fc);
		
		return fc.getFeatureData();
	}
	
	@Test
	public void uniqueIdentifiersCount() throws Exception{
		assertEquals("Unique identifiers count does not match", 6, getFeatureData().getUniqueIdentifiersCount());		
	}
	
	

}
