package neurob.tests.features;

import static org.junit.Assert.*;

import java.io.File;

import org.datavec.api.util.ClassPathResource;
import org.junit.Test;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.PredicateFeatureCollector;
import neurob.core.features.PredicateFeatureData;

public class FeatureDataTest {
	private BParser p;
	private File resource;
	private Start ast;
	private PredicateFeatureData fd;
	
	public FeatureDataTest() throws Exception{
		p = new BParser();
		resource = new ClassPathResource("features_check.mch").getFile();
		ast = p.parseFile(resource, false);
		
		PredicateFeatureCollector fc = new PredicateFeatureCollector();
		ast.apply(fc);
		
		fd = fc.getFeatureData();
	}
	
	@Test
	public void uniqueIdentifiersCount() throws Exception{
		assertEquals("Unique identifiers count does not match", 6, fd.getUniqueIdentifiersCount());		
	}

	@Test
	public void featureDataByASTConstructorTest() throws Exception{
		PredicateFeatureData fd2 = new PredicateFeatureData(ast);
		assertEquals("Features do not match", fd.toString(), fd2.toString());
	}
	
	@Test
	public void featureDataByStringConstructorTest() throws Exception{
		String pred = "x : NATURAL & y : INTEGER & z : NATURAL & z < 20 & a : NAT & b : NAT1 & a < 7 & c : INT";
		PredicateFeatureData fd2 = new PredicateFeatureData(pred);
		assertEquals("Features do not match", fd.toString(), fd2.toString());
	}
	
	

}
