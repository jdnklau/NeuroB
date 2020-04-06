package neurob.tests.features;

import static org.junit.Assert.*;

import java.io.File;

import org.datavec.api.util.ClassPathResource;
import org.junit.Test;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.util.TheoryFeatureCollector;
import neurob.core.features.util.TheoryFeatureData;

public class TheoryFeatureDataTest {
	private BParser p;
	private File resource;
	private Start ast;
	private TheoryFeatureData fd;
	
	public TheoryFeatureDataTest() throws Exception{
		p = new BParser();
		resource = new ClassPathResource("features_check.mch").getFile();
		ast = p.parseFile(resource, false);
		
		TheoryFeatureCollector fc = new TheoryFeatureCollector();
		ast.apply(fc);
		
		fd = fc.getFeatureData();
	}
	
	@Test
	public void uniqueIdentifiersCount() throws Exception{
		assertEquals("Unique identifiers count does not match", 6, fd.getUniqueIdentifiersCount());		
	}
	
	@Test
	public void featureDataByStringConstructorTest() throws Exception{
		String pred = "x : NATURAL & y : INTEGER & z : NATURAL & z < 20 & a : NAT & b : NAT1 & a < 7 & c : INT"
					+ " & # y . (y < x) & ! z . (z < 15 => x > 3)";
		TheoryFeatureData fd2 = new TheoryFeatureData(pred);
		assertEquals("Features do not match", fd.toString(), fd2.toString());
	}
	
	@Test public void predicateFeatureDataLengthTest(){
		int featureLength = fd.toString().split(",").length;
		assertEquals("Number of generated features does not match.", TheoryFeatureData.featureCount, featureLength);
	}
	
	

}
