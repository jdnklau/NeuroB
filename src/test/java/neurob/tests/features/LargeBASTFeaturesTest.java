package neurob.tests.features;

import static org.junit.Assert.*;

import neurob.core.features.util.LargeBASTFeatureData;
import neurob.exceptions.NeuroBException;
import org.junit.Test;

/**
 * @author Jannik Dunkelau
 */
public class LargeBASTFeaturesTest {

	@Test
	public void maxDepthTest() throws NeuroBException {
		String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2)) & x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getFeatureCollector().getMaxDepth();

		assertEquals("Predicate depth does not match", expected, actual);
	}

	@Test
	public void maxDepthTest2() throws NeuroBException {
		String pred = "x : NATURAL & x > 2 & x < 5";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFeatureCollector().getMaxDepth();

		assertEquals("Predicate depth does not match", expected, actual);
	}

	@Test
	public void conjunctsCountTest() throws NeuroBException {
		String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2 & x<9)) & x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getConjunctsCount();

		assertEquals("conjuncts count does not match", expected, actual);
	}

	@Test
	public void conjunctionsCountTest() throws NeuroBException {
		String pred = "x : NATURAL & not(x> 3 => (x>2 or x > 2 or x<9)) & x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 4;
		int actual = data.getConjunctionsCount();

		assertEquals("conjunctions count does not match", expected, actual);
	}

	@Test
	public void disjunctionsCountTest() throws NeuroBException {
		String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2 or x<9 & x>5)) or x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getDisjunctionsCount();

		assertEquals("disjunctions count does not match", expected, actual);
	}

	@Test
	public void implicationsCountTest() throws NeuroBException {
		String pred = "(x : NATURAL & not(x> 3 => (x>2 & x > 2 or x<9))) => x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getImplicationsCount();

		assertEquals("implications count does not match", expected, actual);
	}

	@Test
	public void equivalencesCountTest() throws NeuroBException {
		String pred = "(x : NATURAL <=> not(x> 3 <=> (x>2 & x > 2 or x<9))) <=> x > 2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getEquivalencesCount();

		assertEquals("equivalences count does not match", expected, actual);
	}

	@Test
	public void universalQuantifiersCountTest() throws NeuroBException {
		String pred = "!x.(x : NATURAL => !y . (y>x+2 => y>x+1))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getUniversalQuantifiersCount();

		assertEquals("universal quantifiers count does not match", expected, actual);
	}

	@Test
	public void negatedUniversalQuantifiersCountTest() throws NeuroBException {
		String pred = "not(!x.(x : NATURAL => !y . (y>x+2 => y>x+1)))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 0;
		int actual = data.getUniversalQuantifiersCount();
		assertEquals("universal quantifiers count does not match", expected, actual);

		expected = 2;
		actual = data.getExistentialQuantifiersCount();
		assertEquals("existential quantifiers count does not match", expected, actual);
	}

	@Test
	public void existentialQuantifiersCountTest() throws NeuroBException {
		String pred = "#x.(x : NATURAL & #y . (y>x+2 => y>x+1))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getExistentialQuantifiersCount();

		assertEquals("existential quantifiers count does not match", expected, actual);
	}

	@Test
	public void negatedExistentialQuantifiersCountTest() throws NeuroBException {
		String pred = "not(#x.(x : NATURAL & #y . (y>x+2 => y>x+1)))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 0;
		int actual = data.getExistentialQuantifiersCount();
		assertEquals("existential quantifiers count does not match", expected, actual);

		expected = 2;
		actual = data.getUniversalQuantifiersCount();
		assertEquals("universal quantifiers count does not match", expected, actual);
	}

	@Test
	public void mixedQuantifiersCountTest() throws NeuroBException {
		String pred = "not(!x . (x : NATURAL => " +
				"#y . (y > x & not(#z.(z<y & z=2*y)))))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getExistentialQuantifiersCount();
		assertEquals("existential quantifiers count does not match", expected, actual);

		expected = 1;
		actual = data.getUniversalQuantifiersCount();
		assertEquals("universal quantifiers count does not match", expected, actual);
	}

	@Test
	public void equalityCountTest() throws NeuroBException {
		String pred = "x : NATURAL & y = x & not(!x.(x /= 0 => y > x-1))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getEqualityCount();
		assertEquals("equality count does not match", expected, actual);
	}

	@Test
	public void inequalityCountTest() throws NeuroBException {
		String pred = "x : NATURAL & y /= x & not(!x.(x = 0 => y > x-1))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getInequalityCount();
		assertEquals("inequality count does not match", expected, actual);
	}

	@Test
	public void setMembersCountTest() throws NeuroBException {
		String pred = "x : NATURAL & not(x /: NAT)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getMemberCount();
		assertEquals("set membership count does not match", expected, actual);
	}

	@Test
	public void setNotMembersCountTest() throws NeuroBException {
		String pred = "x /: NATURAL & not(x : NAT)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getNotMemberCount();
		assertEquals("set non-membership count does not match", expected, actual);
	}

	@Test
	public void subsetCountTest() throws NeuroBException {
		String pred = "POW(NAT) <: POW(NATURAL) & not(NAT /<<: NATURAL)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getSubsetCount();
		assertEquals("subset count does not match", expected, actual);
	}

	@Test
	public void notSubsetCountTest() throws NeuroBException {
		String pred = "POW(NAT) /<: POW(NATURAL) & not(NAT <<: NATURAL)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getNotSubsetCount();
		assertEquals("non-subset count does not match", expected, actual);
	}

	@Test
	public void comparisonCountTest() throws NeuroBException {
		String pred = "42<43 & 3>1 & 4 >=2 & 101 <= 1337";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 4;
		int actual = data.getSizeComparisonCount();
		assertEquals("comparison count does not match", expected, actual);
	}

	@Test
	public void booleanLiteralsCountTest() throws NeuroBException {
		String pred = "FALSE /= TRUE & TRUE = TRUE";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 4;
		int actual = data.getBooleanLiteralsCount();
		assertEquals("boolean literals count does not match", expected, actual);
	}

//	@Test
//	public void finiteSetsCountTest() throws NeuroBException {
//		String pred = "finite({})";
//		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);
//
//		int expected = 1;
//		int actual = data.getFiniteSetRequirementsCount();
//		assertEquals("boolean literals count does not match", expected, actual);
//	}




}
