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

	@Test
	public void booleanConversionsCountTest() throws NeuroBException {
		String pred = "x:BOOL & bool(2>3) = x & bool(2>3) /= x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getBooleanConversionCount();
		assertEquals("boolean conversion count does not match", expected, actual);
	}

	@Test
	public void finiteSetsCountTest() throws NeuroBException {
		String pred = "S=1..10 & S:FIN(S) & S:FIN1(S)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getFiniteSetRequirementsCount();
		assertEquals("finite sets count does not match", expected, actual);
	}

	@Test
	public void infiniteSetsCountTest() throws NeuroBException {
		String pred = "not(S=1..10 & S:FIN(S) & S:FIN1(S))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getInfiniteSetRequirementsCount();
		assertEquals("infinite sets count does not match", expected, actual);
	}

	@Test
	public void arithmeticsCountTest() throws NeuroBException {
		String pred = "x:NATURAL & y:NATURAL & z:INTEGER & x+2*y-z*y+x/x/y/z = 7";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getArithmeticAdditionCount();
		assertEquals("addition count does not match", expected, actual);

		expected = 2;
		actual = data.getArithmeticMultiplicationCount();
		assertEquals("multiplication count does not match", expected, actual);

		expected = 3;
		actual = data.getArithmeticDivisionCount();
		assertEquals("division count does not match", expected, actual);

//		expected = 1;
//		actual = data.getArithmeticModuloCount();
//		assertEquals("modulo count does not match", expected, actual);
	}

	@Test
	public void moduloCountTest() throws NeuroBException {
		String pred = "x:NATURAL & y:NATURAL & z:INTEGER & 1 = x mod y & z = y mod x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getArithmeticModuloCount();
		assertEquals("modulo count does not match", expected, actual);
	}

	@Test
	public void exponentialCountTest() throws NeuroBException {
		String pred = "x:NATURAL & y:NATURAL & z:INTEGER & x**y = z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getArithmeticExponentialCount();
		assertEquals("exponential operations count does not match", expected, actual);
	}

	@Test
	public void minimumCountTest() throws NeuroBException {
		String pred = "S = {1,2} & x = min(S)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getArithmeticMinCount();
		assertEquals("minimum operations count does not match", expected, actual);
	}

	@Test
	public void maximumCountTest() throws NeuroBException {
		String pred = "x = max(S)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getArithmeticMaxCount();
		assertEquals("maximum operations count does not match", expected, actual);
	}

	@Test
	public void identifierCountTest() throws NeuroBException {
		String pred = "x : NAT & y < z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifiersCount();
		assertEquals("Amount of identifiers does not match", expected, actual);
	}

	@Test
	public void identifierRelationsCountTest() throws NeuroBException {
		String pred = "x : NAT & y < z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getIdentifierRelationsCount();
		assertEquals("Amount of identifier relations does not match", expected, actual);
	}

	@Test
	public void identifierRelationsCount2Test() throws NeuroBException {
		String pred = "x < y & y < z & x < z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifierRelationsCount();
		assertEquals("Amount of identifier relations does not match", expected, actual);
	}

	@Test
	public void identifierImplicitRelationsCountTest() throws NeuroBException {
		String pred = "y < z & x < y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifierRelationsCount();
		assertEquals("Amount of identifier relations does not match", expected, actual);
	}

	@Test
	public void identifierImplicitRelationsCount2Test() throws NeuroBException {
		String pred = "x < y & y < z & z < x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifierRelationsCount();
		assertEquals("Amount of identifier relations does not match", expected, actual);
	}

	@Test
	public void identifierBoundedCountTest() throws NeuroBException {
		String pred = "x : NAT & y : NATURAL & z : INT & x > 0 & x < z & z < y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1; // todo: should x>0 count as symbolic boundary?
		int actual = data.getIdentifierBoundedCount();
		assertEquals("Amount of bounded identifiers does not match", expected, actual);
	}

	@Test
	public void identifierBoundedCount2Test() throws NeuroBException {
		String pred = "x : NAT & y : NATURAL & z : INT & x = 2*y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierBoundedCount();
		assertEquals("Amount of bounded identifiers does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedCountTest() throws NeuroBException {
		String pred = "x : NAT & y : NATURAL & z : INT & x < z & z < y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedCount();
		assertEquals("Amount of semi-bounded identifiers does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedCount2Test() throws NeuroBException {
		String pred = "x : INTEGER & x > y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedCount();
		assertEquals("Amount of semi-bounded identifiers does not match", expected, actual);
	}

	@Test
	public void identifierUnboundedCountTest() throws NeuroBException {
		String pred = "x : NAT & y : NATURAL & z : INT & x < z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getIdentifierUnboundedCount();
		assertEquals("Amount of unbounded identifiers does not match", expected, actual);
	}

	@Test
	public void identifierBoundedDomainCountTest() throws NeuroBException {
		String pred = "x : NAT & y : NATURAL & z : INT";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierBoundedDomainCount();
		assertEquals("Amount of bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierBoundedDomainCount2Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & z : INT & y < z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierBoundedDomainCount();
		assertEquals("Amount of bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierBoundedDomainCount3Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & z : INT & y < z & x = y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifierBoundedDomainCount();
		assertEquals("Amount of bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierBoundedDomainCount4Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & z : INT & y < z & x /= y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierBoundedDomainCount();
		assertEquals("Amount of bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierBoundedDomainCount5Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & z : INT & not(y >= z & x /= y)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getIdentifierBoundedDomainCount();
		assertEquals("Amount of bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCountTest() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & z : INT & y > z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCount2Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & x = y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCount3Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & x > 2*y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCount4Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & x >= 2*y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCount5Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & 2*y < x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierSemiBoundedDomainCount6Test() throws NeuroBException {
		String pred = "x : INTEGER & y : NATURAL & 2*y <= x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getIdentifierSemiBoundedDomainCount();
		assertEquals("Amount of semi-bounded domains does not match", expected, actual);
	}

	@Test
	public void identifierUnboundedDomainCountTest() throws NeuroBException {
		String pred = "x : NAT1 & y : INTEGER & z : INT";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getIdentifierUnboundedDomainCount();
		assertEquals("Amount of unbounded domains does not match", expected, actual);
	}



}
