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
	public void generalisedSumCountTest() throws NeuroBException {
		String pred = "2 = SIGMA(z).(z:NATURAL1 | 1/z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getArithmeticGeneralisedSumCount();
		assertEquals("generalised sum count does not match", expected, actual);
	}

	@Test
	public void generalisedProdCountTest() throws NeuroBException {
		String pred = "0 = PI(z).(z:NATURAL1 | 1/z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getArithmeticGeneralisedProductCount();
		assertEquals("generalised product count does not match", expected, actual);
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
	public void identifierPrimedCountTest() throws NeuroBException {
		String pred = "x$0 : NAT & y < z";
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
	public void identifierImplicitRelations3Count2Test() throws NeuroBException {
		String pred = "x < y$0 & y$0 < z & z < x";
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
	public void identifierBoundedDomainCount6Test() throws NeuroBException {
		String pred = "x : 1..10 & y : -100..100";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
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

	@Test
	public void powerSetCountTest() throws NeuroBException{
		String pred = "x : NAT & y : POW1(POW(NAT)) & z : POW1(NAT)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getPowerSetCount();
		assertEquals("Amount of power sets does not match", expected, actual);
	}

	@Test
	public void powerSetMaxDepthCountTest() throws NeuroBException{
		String pred = "x : NAT & y : POW(POW1(POW(NAT))) & z : POW(POW1(NAT))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);


		int expected = 3;
		int actual = data.getFeatureCollector().getPowMaxDepth();
		assertEquals("Power set max depth does not match", expected, actual);
	}

	@Test
	public void powerSetMaxDepthCount2Test() throws NeuroBException{
		String pred = "x : NAT & y : POW(POW1(POW(NAT))*POW1(INTEGER)) & z : POW(POW1(NAT))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);


		int expected = 3;
		int actual = data.getFeatureCollector().getPowMaxDepth();
		assertEquals("Power set max depth does not match", expected, actual);
	}

	@Test
	public void powerSetMaxDepthCount3Test() throws NeuroBException{
		String pred = "x : NAT & y : POW(POW1(NAT)*POW1(POW(INTEGER))) & z : POW(POW1(NAT))";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);


		int expected = 3;
		int actual = data.getFeatureCollector().getPowMaxDepth();
		assertEquals("Power set max depth does not match", expected, actual);
	}

	@Test
	public void powerSetHigherOrderTest() throws NeuroBException{
		String pred = "x : NAT & y : POW1(POW(NAT)) & z : POW1(NAT)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getPowerSetHigherOrderCounts();
		assertEquals("Amount of power set stacks does not match", expected, actual);
	}

	@Test
	public void powerSetHigherOrder2Test() throws NeuroBException{
		String pred = "x : NAT & y : POW1(NAT*POW(NAT)) & z : POW1(NAT)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getPowerSetHigherOrderCounts();
		assertEquals("Amount of power set stacks does not match", expected, actual);
	}

	@Test
	public void setCardCountTest() throws NeuroBException{
		String pred = "card(NAT) > 3";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetCardCount();
		assertEquals("Cardinality count does not match", expected, actual);
	}

	@Test
	public void setUnionCountTest() throws NeuroBException{
		String pred = "x \\/ y = z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetUnionCount();
		assertEquals("Amount of set unions does not match", expected, actual);
	}

	@Test
	public void setUnionCount2Test() throws NeuroBException{
		String pred = "x \\/ y \\/ a = z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getSetUnionCount();
		assertEquals("Amount of set unions does not match", expected, actual);
	}

	@Test
	public void setUnionCount3Test() throws NeuroBException{
		String pred = "x = union({a,b,c,d})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetUnionCount();
		assertEquals("Amount of set unions does not match", expected, actual);
	}

	@Test
	public void setUnionCount4Test() throws NeuroBException{
		String pred = "x = UNION(z).(card(z)=3 & z<:NAT | z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetUnionCount();
		assertEquals("Amount of set unions does not match", expected, actual);
	}

	@Test
	public void setIntersecCountTest() throws NeuroBException{
		String pred = "x /\\ y = z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetIntersectCount();
		assertEquals("Amount of set intersections does not match", expected, actual);
	}

	@Test
	public void setIntersecCount2Test() throws NeuroBException{
		String pred = "x /\\ y /\\ a = z";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getSetIntersectCount();
		assertEquals("Amount of set intersections does not match", expected, actual);
	}

	@Test
	public void setIntersecCount3Test() throws NeuroBException{
		String pred = "x = inter({a,b,c,d})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetIntersectCount();
		assertEquals("Amount of set intersections does not match", expected, actual);
	}

	@Test
	public void setIntersecCount4Test() throws NeuroBException{
		String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetIntersectCount();
		assertEquals("Amount of set intersections does not match", expected, actual);
	}

	@Test
	public void setGeneralUnionCountTest() throws NeuroBException{
		String pred = "x = union({}) & y = union({a,b,c,d}) & z = union(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getSetGeneralUnionCount();
		assertEquals("Amount of generalised unions does not match", expected, actual);
	}

	@Test
	public void setQualifiedUnionCountTest() throws NeuroBException{
		String pred = "x = UNION(z).(card(z)=3 & z<:NAT | z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetQuantifiedUnionCount();
		assertEquals("Amount of generalised unions does not match", expected, actual);
	}

	@Test
	public void setGeneralIntersectCountTest() throws NeuroBException{
		String pred = "x = inter({}) & y = inter({a,b,c,d}) & z = inter(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getSetGeneralIntersectCount();
		assertEquals("Amount of generalised intersections does not match", expected, actual);
	}

	@Test
	public void setQualifiedIntersectCountTest() throws NeuroBException{
		String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetQuantifiedIntersectCount();
		assertEquals("Amount of generalised intersections does not match", expected, actual);
	}

	@Test
	public void setSubtractionCountTest() throws NeuroBException{
		String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z) & y : FIN(NATURAL) & z = y\\x";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetQuantifiedIntersectCount();
		assertEquals("Amount of generalised intersections does not match", expected, actual);
	}

	@Test
	public void setComprehensionCountTest() throws NeuroBException{
		String pred = "X = {x | y:NATURAL1 & x=y**2}";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSetComprehensionCount();
		assertEquals("Amount of set comprehensions does not match", expected, actual);
	}

	@Test
	public void relCountTest() throws NeuroBException{
		String pred = "x : A<->B & y : POW(A<->B) & z : A<->(A<->B)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 4;
		int actual = data.getRelationCount();
		assertEquals("Amount of relations does not match", expected, actual);
	}

	@Test
	public void totalRelCountTest() throws NeuroBException{
		String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getRelationTotalCount();
		assertEquals("Amount of total relations does not match", expected, actual);
	}

	@Test
	public void surjRelCountTest() throws NeuroBException{
		String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getRelationSurjCount();
		assertEquals("Amount of surjective relations does not match", expected, actual);
	}

	@Test
	public void totalSurjRelCountTest() throws NeuroBException{
		String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRelationTotalSurjCount();
		assertEquals("Amount of total surjective relations does not match", expected, actual);
	}

	@Test
	public void relImageCountTest() throws NeuroBException{
		String pred = "r : S<->T & S2 <: S & t = r[S2] & u = r[{1}]";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getRelationalImageCount();
		assertEquals("Amount of relational images does not match", expected, actual);
	}

	@Test
	public void relInverseCountTest() throws NeuroBException{
		String pred = "x : A<<->B & y = x~";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRelationInverseCount();
		assertEquals("Amount of inverse relations does not match", expected, actual);
	}

	@Test
	public void relOverrideCountTest() throws NeuroBException{
		String pred = "r1 : A<->B & r2 : A<->B & r = r1 <+ r2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRelationOverrideCount();
		assertEquals("Amount of relational overrides does not match", expected, actual);
	}

	@Test
	public void relParallelProductCountTest() throws NeuroBException{
		String pred = "r1 : A<->B & r2 : A<->B & r = (r1 || r2)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRelationParallelProductCount();
		assertEquals("Amount of relational parallel products does not match", expected, actual);
	}

	@Test
	public void relDirectProductCountTest() throws NeuroBException{
		String pred = "r1 : A<->B & r2 : A<->B & r = r1 >< r2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRelationDirectProductCount();
		assertEquals("Amount of relational direct products does not match", expected, actual);
	}

	@Test
	public void relProj1CountTest() throws NeuroBException{
		String pred = "x = prj1(x,y) & y = prj2(x,y) & z = prj2(z,z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getProjection1Count();
		assertEquals("Amount of first projections does not match", expected, actual);
	}

	@Test
	public void relProj2CountTest() throws NeuroBException{
		String pred = "x = prj1(x,y) & y = prj2(x,y) & z = prj2(z,z)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getProjection2Count();
		assertEquals("Amount of second projections does not match", expected, actual);
	}

	@Test
	public void relForwardCompCountTest() throws NeuroBException{
		String pred = "x = {(1,2)} & y = {(2,3)} & z = (x ; y)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getForwardCompositionCount();
		assertEquals("Amount of forward compositions does not match", expected, actual);
	}

	@Test
	public void relForwardCompCount2Test() throws NeuroBException{
		String pred = "x : S<->T & y : S<->T & z = (x ; y)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getForwardCompositionCount();
		assertEquals("Amount of forward compositions does not match", expected, actual);
	}

	@Test
	public void domainRestrictionCountTest() throws NeuroBException{
		String pred = "r : S<->T & S2 <<: S & r2 = S2 <| r";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getDomainRestrictionCount();
		assertEquals("Amount of domain restrictions does not match", expected, actual);
	}

	@Test
	public void domainSubtractionCountTest() throws NeuroBException{
		String pred = "r : S<->T & S2 <<: S & r2 = S2 <<| r";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getDomainSubtractionCount();
		assertEquals("Amount of domain subtractions does not match", expected, actual);
	}

	@Test
	public void rangeRestrictionCountTest() throws NeuroBException{
		String pred = "r : S<->T & S2 <<: S & r2 = r |> S2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRangeRestrictionCount();
		assertEquals("Amount of range restrictions does not match", expected, actual);
	}

	@Test
	public void rangeSubtractionCountTest() throws NeuroBException{
		String pred = "r : S<->T & S2 <<: S & r2 = r |>> S2";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRangeSubtractionCount();
		assertEquals("Amount of range subtractions does not match", expected, actual);
	}

	@Test
	public void partialFunCountTest() throws NeuroBException{
		String pred = "f : A +-> B & g : B --> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunPartialCount();
		assertEquals("Amount of partial functions does not match", expected, actual);
	}

	@Test
	public void totalFunCountTest() throws NeuroBException{
		String pred = "f : A +-> B & g : B --> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunTotalCount();
		assertEquals("Amount of total functions does not match", expected, actual);
	}

	@Test
	public void partialInjFunCountTest() throws NeuroBException{
		String pred = "f : A >-> B & g : B >+> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunPartialInjCount();
		assertEquals("Amount of partial injective functions does not match", expected, actual);
	}

	@Test
	public void totalInjFunCountTest() throws NeuroBException{
		String pred = "f : A >-> B & g : B >+> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunTotalInjCount();
		assertEquals("Amount of total injective functions does not match", expected, actual);
	}

	@Test
	public void partialSurjFunCountTest() throws NeuroBException{
		String pred = "f : A -->> B & g : B +->> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunPartialSurjCount();
		assertEquals("Amount of partial surjective functions does not match", expected, actual);
	}

	@Test
	public void totalSurjFunCountTest() throws NeuroBException{
		String pred = "f : A -->> B & g : B +->> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunTotalSurjCount();
		assertEquals("Amount of total surjective functions does not match", expected, actual);
	}

	@Test
	public void partialBijFunCountTest() throws NeuroBException{
		String pred = "f : A >->> B & g : B >+>> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunPartialBijCount();
		assertEquals("Amount of partial bijective functions does not match", expected, actual);
	}

	@Test
	public void totalBijFunCountTest() throws NeuroBException{
		String pred = "f : A >->> B & g : B >+>> A";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFunTotalBijCount();
		assertEquals("Amount of total bijective functions does not match", expected, actual);
	}

	@Test
	public void funApplicationCountTest() throws NeuroBException{
		String pred = "f : A >->> B & g : B >+>> A & x = f(1) & y = f(2)*f(3)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getFunctionApplicationCount();
		assertEquals("Amount of function applications does not match", expected, actual);
	}

	@Test
	public void lambdaFunCountTest() throws NeuroBException{
		String pred = "X = %x.(x:NATURAL|x**2)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getLambdaCount();
		assertEquals("Amount of lambda abstractions does not match", expected, actual);
	}

	@Test
	public void seqCountTest() throws NeuroBException{
		String pred = "x = [1,2,3]";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSeqCount();
		assertEquals("Amount of sequences does not match", expected, actual);
	}

	@Test
	public void seqCount2Test() throws NeuroBException{
		String pred = "x : seq({1,2,3})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSeqCount();
		assertEquals("Amount of sequences does not match", expected, actual);
	}

	@Test
	public void seqCount3Test() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y : seq({1,2,3})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getSeqCount();
		assertEquals("Amount of sequences does not match", expected, actual);
	}

	@Test
	public void seqCount4Test() throws NeuroBException{
		String pred = "x : iseq({1,2,3})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getSeqCount();
		assertEquals("Amount of isequences does not match", expected, actual);
	}

	@Test
	public void seqCount5Test() throws NeuroBException{
		String pred = "x : iseq1({1,2,3}) & y : iseq({1,2,3})";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 2;
		int actual = data.getSeqCount();
		assertEquals("Amount of isequences does not match", expected, actual);
	}

	@Test
	public void seqTailCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getTailCount();
		assertEquals("Amount of sequence tails does not match", expected, actual);
	}

	@Test
	public void seqFirstCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFirstCount();
		assertEquals("Amount of sequence heads does not match", expected, actual);
	}

	@Test
	public void seqLastCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getLastCount();
		assertEquals("Amount of sequence lasts does not match", expected, actual);
	}

	@Test
	public void seqFrontCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = front(x) & first(x) = last(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFrontCount();
		assertEquals("Amount of sequence fronts does not match", expected, actual);
	}

	@Test
	public void seqInsertFrontCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = x <- 4 & z = 0 -> y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFrontInsertionCount();
		assertEquals("Amount of sequence front insertions does not match", expected, actual);
	}

	@Test
	public void seqInsertTailCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = x <- 4 & z = 0 -> y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getTailInsertionCount();
		assertEquals("Amount of sequence tail insertions does not match", expected, actual);
	}

	@Test
	public void seqRestrictFrontCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = x \\|/ 4 & z = y /|\\ 3";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getFrontRestrictionCount();
		assertEquals("Amount of sequence front restrictions does not match", expected, actual);
	}

	@Test
	public void seqRestrictTailCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = x \\|/ 4 & z = y /|\\ 3";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getTailRestrictionCount();
		assertEquals("Amount of sequence tail restrictions does not match", expected, actual);
	}

	@Test
	public void seqReverseCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & x /= rev(x)";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getRevCount();
		assertEquals("Amount of sequence reversions does not match", expected, actual);
	}

	@Test
	public void seqConcatCountTest() throws NeuroBException{
		String pred = "x : seq1({1,2,3}) & y = x^x & z = y^x^y";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 3;
		int actual = data.getConcatCount();
		assertEquals("Amount of sequence concatenations does not match", expected, actual);
	}

	@Test
	public void seqGeneralConcatCountTest() throws NeuroBException{
		String pred = "S=seq(1..20) & x:S & y:S & z:S & u = conc([x,y,z])";
		LargeBASTFeatureData data = new LargeBASTFeatureData(pred);

		int expected = 1;
		int actual = data.getGeneralConcatCount();
		assertEquals("Amount of general sequence concatenations does not match", expected, actual);
	}






}
