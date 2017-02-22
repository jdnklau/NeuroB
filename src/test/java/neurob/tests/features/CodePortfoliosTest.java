package neurob.tests.features;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import org.junit.Test;

import neurob.core.features.CodeImages;
import neurob.exceptions.NeuroBException;

public class CodePortfoliosTest {

	@Test
	public void translationOfStringAndImageTest() throws NeuroBException {
		String pred = "x : NATURAL & y : INTEGER & z : NATURAL & z < 20 & a : NAT & b : NAT1 & a < 7 & c : INT"
				+ " & # y . (y < x) & ! z . (z < 15 => x > 3)";
		
		CodeImages cp = new CodeImages(32);
		
		BufferedImage img = cp.generateFeatureImage(pred);
		
		String expected = cp.generateFeatureString(pred);
		String actual = cp.translateImageFeatureToString(img);
		
		assertEquals("Representation of feature string and translated image to string does not match", expected, actual);
		
		// test if it is really the inverse
		String actual2 = cp.translateImageFeatureToString(cp.translateStringFeatureToImage(expected));
		assertEquals("Translations image2string and string2image are not completely inverse to one another", expected, actual2);
		
	}

}
