package neurob.tests.features;

import static org.junit.Assert.*;

import neurob.core.features.BASTFullFeatures;
import neurob.exceptions.NeuroBException;
import org.junit.Test;

/**
 * @author Jannik Dunkelau
 */
public class BASTFullFeaturesTest {

	@Test
	public void featureDimTest() throws NeuroBException {
		int expected = BASTFullFeatures.featureDimension;
		int actual = new BASTFullFeatures().generateFeatureArray("TRUE = TRUE").length;

		assertEquals("Amount of features does not match", expected, actual);
	}
}
