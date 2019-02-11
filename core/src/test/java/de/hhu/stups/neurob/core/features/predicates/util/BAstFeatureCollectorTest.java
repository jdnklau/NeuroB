package de.hhu.stups.neurob.core.features.predicates.util;

import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.exceptions.FeatureCreationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BAstFeatureCollectorTest {

    @Test
    public void maxDepthTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2)) & x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getMaxDepth();

        assertEquals(expected, actual, "Predicate depth does not match");
    }

    @Test
    public void maxDepthTest2() throws FeatureCreationException {
        String pred = "x : NATURAL & x > 2 & x < 5";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getMaxDepth();

        assertEquals(expected, actual, "Predicate depth does not match");
    }

    @Test
    public void conjunctsCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2 & x<9)) & x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getConjunctsCount();

        assertEquals(expected, actual, "conjuncts count does not match");
    }

    @Test
    public void conjunctsCount2Test() throws FeatureCreationException {
        String pred = "x : NATURAL & (not(x> 3 => (x>2 & x > 2 & x<9))) & (x > 2)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getConjunctsCount();

        assertEquals(expected, actual, "conjuncts count does not match");
    }

    @Test
    public void conjunctionsCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x> 3 => (x>2 or x > 2 or x<9)) & x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 4;
        int actual = data.getConjunctionsCount();

        assertEquals(expected, actual, "conjunctions count does not match");
    }

    @Test
    public void disjunctionsCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x> 3 => (x>2 & x > 2 or x<9 & x>5)) or x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getDisjunctionsCount();

        assertEquals(expected, actual, "disjunctions count does not match");
    }

    @Test
    public void negationCount2Test() throws FeatureCreationException {
        String pred = "x : NATURAL & x/:2..7 & not(x/=8)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getNegationCount();

        assertEquals(expected, actual, "negation count does not match");
    }

    @Test
    public void implicationsCountTest() throws FeatureCreationException {
        String pred = "(x : NATURAL & not(x> 3 => (x>2 & x > 2 or x<9))) => x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getImplicationsCount();

        assertEquals(expected, actual, "implications count does not match");
    }

    @Test
    public void negationDepthCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x:2..7 or not(x=8))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getNegationMaxDepth();

        assertEquals(expected, actual, "negation count does not match");
    }

    @Test
    public void negationDepthCount2Test() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x:2..7 or x=8)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getNegationMaxDepth();

        assertEquals(expected, actual, "negation count does not match");
    }

    @Test
    public void equivalencesCountTest() throws FeatureCreationException {
        String pred = "(x : NATURAL <=> not(x> 3 <=> (x>2 & x > 2 or x<9))) <=> x > 2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getEquivalencesCount();

        assertEquals(expected, actual, "equivalences count does not match");
    }

    @Test
    public void universalQuantifiersCountTest() throws FeatureCreationException {
        String pred = "!x.(x : NATURAL => !y . (y>x+2 => y>x+1))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getUniversalQuantifiersCount();

        assertEquals(expected, actual, "universal quantifiers count does not match");
    }

    @Test
    public void negatedUniversalQuantifiersCountTest() throws FeatureCreationException {
        String pred = "not(!x.(x : NATURAL => !y . (y>x+2 => y>x+1)))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 0;
        int actual = data.getUniversalQuantifiersCount();
        assertEquals(expected, actual, "universal quantifiers count does not match");

        expected = 2;
        actual = data.getExistentialQuantifiersCount();
        assertEquals(expected, actual, "existential quantifiers count does not match");
    }

    @Test
    public void existentialQuantifiersCountTest() throws FeatureCreationException {
        String pred = "#x.(x : NATURAL & #y . (y>x+2 => y>x+1))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getExistentialQuantifiersCount();

        assertEquals(expected, actual, "existential quantifiers count does not match");
    }

    @Test
    public void negatedExistentialQuantifiersCountTest() throws FeatureCreationException {
        String pred = "not(#x.(x : NATURAL & #y . (y>x+2 => y>x+1)))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 0;
        int actual = data.getExistentialQuantifiersCount();
        assertEquals(expected, actual, "existential quantifiers count does not match");

        expected = 2;
        actual = data.getUniversalQuantifiersCount();
        assertEquals(expected, actual, "universal quantifiers count does not match");
    }

    @Test
    public void mixedQuantifiersCountTest() throws FeatureCreationException {
        String pred = "not(!x . (x : NATURAL => " +
                      "#y . (y > x & not(#z.(z<y & z=2*y)))))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getExistentialQuantifiersCount();
        assertEquals(expected, actual, "existential quantifiers count does not match");

        expected = 1;
        actual = data.getUniversalQuantifiersCount();
        assertEquals(expected, actual, "universal quantifiers count does not match");
    }

    @Test
    public void quantifierDepthCountTest() throws FeatureCreationException {
        String pred = "not(!x . (x : NATURAL => " +
                      "#y . (y > x & not(#z.(z<y & z=2*y)))))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getQuantifierMaxDepthCount();
        assertEquals(expected, actual, "quantifier nesting depth does not match");
    }

    @Test
    public void quantifierDepthCount2Test() throws FeatureCreationException {
        String pred = "not(!x . (x : NATURAL => " +
                      "#y . (y > x)))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getQuantifierMaxDepthCount();
        assertEquals(expected, actual, "quantifier nesting depth does not match");
    }

    @Test
    public void equalityCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & y = x & not(!x.(x /= 0 => y > x-1))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getEqualityCount();
        assertEquals(expected, actual, "equality count does not match");
    }

    @Test
    public void inequalityCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & y /= x & not(!x.(x = 0 => y > x-1))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getInequalityCount();
        assertEquals(expected, actual, "inequality count does not match");
    }

    @Test
    public void setMembersCountTest() throws FeatureCreationException {
        String pred = "x : NATURAL & not(x /: NAT)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getMemberCount();
        assertEquals(expected, actual, "set membership count does not match");
    }

    @Test
    public void setNotMembersCountTest() throws FeatureCreationException {
        String pred = "x /: NATURAL & not(x : NAT)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getNotMemberCount();
        assertEquals(expected, actual, "set non-membership count does not match");
    }

    @Test
    public void subsetCountTest() throws FeatureCreationException {
        String pred = "POW(NAT) <: POW(NATURAL) & not(NAT /<<: NATURAL)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getSubsetCount();
        assertEquals(expected, actual, "subset count does not match");
    }

    @Test
    public void notSubsetCountTest() throws FeatureCreationException {
        String pred = "POW(NAT) /<: POW(NATURAL) & not(NAT <<: NATURAL)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getNotSubsetCount();
        assertEquals(expected, actual, "non-subset count does not match");
    }

    @Test
    public void comparisonCountTest() throws FeatureCreationException {
        String pred = "42<43 & 3>1 & 4 >=2 & 101 <= 1337";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 4;
        int actual = data.getSizeComparisonCount();
        assertEquals(expected, actual, "comparison count does not match");
    }

    @Test
    public void booleanLiteralsCountTest() throws FeatureCreationException {
        String pred = "FALSE /= TRUE & TRUE = TRUE";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 4;
        int actual = data.getBooleanLiteralsCount();
        assertEquals(expected, actual, "boolean literals count does not match");
    }

    @Test
    public void booleanConversionsCountTest() throws FeatureCreationException {
        String pred = "x:BOOL & bool(2>3) = x & bool(2>3) /= x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getBooleanConversionCount();
        assertEquals(expected, actual, "boolean conversion count does not match");
    }

    @Test
    public void finiteSetsCountTest() throws FeatureCreationException {
        String pred = "S=1..10 & S:FIN(S) & S:FIN1(S)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getFiniteSetRequirementsCount();
        assertEquals(expected, actual, "finite sets count does not match");
    }

    @Test
    public void infiniteSetsCountTest() throws FeatureCreationException {
        String pred = "not(S=1..10 & S:FIN(S) & S:FIN1(S))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getInfiniteSetRequirementsCount();
        assertEquals(expected, actual, "infinite sets count does not match");
    }

    @Test
    public void arithmeticsCountTest() throws FeatureCreationException {
        String pred = "x:NATURAL & y:NATURAL & z:INTEGER & x+2*y-z*y+x/x/y/z = 7";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getArithmeticAdditionCount();
        assertEquals(expected, actual, "addition count does not match");

        expected = 2;
        actual = data.getArithmeticMultiplicationCount();
        assertEquals(expected, actual, "multiplication count does not match");

        expected = 3;
        actual = data.getArithmeticDivisionCount();
        assertEquals(expected, actual, "division count does not match");

//		expected = 1;
//		actual = data.getArithmeticModuloCount();
//		assertEquals(expected, actual, "modulo count does not match");
    }

    @Test
    public void moduloCountTest() throws FeatureCreationException {
        String pred = "x:NATURAL & y:NATURAL & z:INTEGER & 1 = x mod y & z = y mod x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getArithmeticModuloCount();
        assertEquals(expected, actual, "modulo count does not match");
    }

    @Test
    public void exponentialCountTest() throws FeatureCreationException {
        String pred = "x:NATURAL & y:NATURAL & z:INTEGER & x**y = z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getArithmeticExponentialCount();
        assertEquals(expected, actual, "exponential operations count does not match");
    }

    @Test
    public void minimumCountTest() throws FeatureCreationException {
        String pred = "S = {1,2} & x = min(S)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getArithmeticMinCount();
        assertEquals(expected, actual, "minimum operations count does not match");
    }

    @Test
    public void maximumCountTest() throws FeatureCreationException {
        String pred = "x = max(S)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getArithmeticMaxCount();
        assertEquals(expected, actual, "maximum operations count does not match");
    }

    @Test
    public void generalisedSumCountTest() throws FeatureCreationException {
        String pred = "2 = SIGMA(z).(z:NATURAL1 | 1/z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getArithmeticGeneralisedSumCount();
        assertEquals(expected, actual, "generalised sum count does not match");
    }

    @Test
    public void generalisedProdCountTest() throws FeatureCreationException {
        String pred = "0 = PI(z).(z:NATURAL1 | 1/z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getArithmeticGeneralisedProductCount();
        assertEquals(expected, actual, "generalised product count does not match");
    }

    @Test
    public void successorCountTest() throws FeatureCreationException {
        String pred = "x = succ(1) & y = succ(succ(x))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getSuccCount();
        assertEquals(expected, actual, "successor count does not match");
    }

    @Test
    public void predecessorCountTest() throws FeatureCreationException {
        String pred = "x = succ(1) & y = pred(pred(x))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getPredecCount();
        assertEquals(expected, actual, "predecessor count does not match");
    }

    @Test
    public void identifierCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifiersCount();
        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void identifierPrimedCountTest() throws FeatureCreationException {
        String pred = "x$0 : NAT & y < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifiersCount();
        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void identifierRelationsCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getIdentifierRelationsCount();
        assertEquals(expected, actual, "Amount of identifier relations does not match");
    }

    @Test
    public void identifierRelationsCount2Test() throws FeatureCreationException {
        String pred = "x < y & y < z & x < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierRelationsCount();
        assertEquals(expected, actual, "Amount of identifier relations does not match");
    }

    @Test
    public void identifierImplicitRelationsCountTest() throws FeatureCreationException {
        String pred = "y < z & x < y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierRelationsCount();
        assertEquals(expected, actual, "Amount of identifier relations does not match");
    }

    @Test
    public void identifierImplicitRelationsCount2Test() throws FeatureCreationException {
        String pred = "x < y & y < z & z < x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierRelationsCount();
        assertEquals(expected, actual, "Amount of identifier relations does not match");
    }

    @Test
    public void identifierImplicitRelations3Count2Test() throws FeatureCreationException {
        String pred = "x < y$0 & y$0 < z & z < x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierRelationsCount();
        assertEquals(expected, actual, "Amount of identifier relations does not match");
    }

    @Test
    public void identifierBoundedCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : NATURAL & z : INT & x > 0 & x < z & z < y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1; // todo: should x>0 count as symbolic boundary?
        int actual = data.getIdentifierBoundedCount();
        assertEquals(expected, actual, "Amount of bounded identifiers does not match");
    }

    @Test
    public void identifierBoundedCount2Test() throws FeatureCreationException {
        String pred = "x : NAT & y : NATURAL & z : INT & x = 2*y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierBoundedCount();
        assertEquals(expected, actual, "Amount of bounded identifiers does not match");
    }

    @Test
    public void identifierSemiBoundedCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : NATURAL & z : INT & x < z & z < y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedCount();
        assertEquals(expected, actual, "Amount of semi-bounded identifiers does not match");
    }

    @Test
    public void identifierSemiBoundedCount2Test() throws FeatureCreationException {
        String pred = "x : INTEGER & x > y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedCount();
        assertEquals(expected, actual, "Amount of semi-bounded identifiers does not match");
    }

    @Test
    public void identifierUnboundedCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : NATURAL & z : INT & x < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getIdentifierUnboundedCount();
        assertEquals(expected, actual, "Amount of unbounded identifiers does not match");
    }

    @Test
    public void identifierBoundedDomainCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : NATURAL & z : INT";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierBoundedDomainCount2Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & z : INT & y < z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierBoundedDomainCount3Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & z : INT & y < z & x = y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierBoundedDomainCount4Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & z : INT & y < z & x /= y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierBoundedDomainCount5Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & z : INT & not(y >= z & x /= y)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierBoundedDomainCount6Test() throws FeatureCreationException {
        String pred = "x : 1..10 & y : -100..100";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierBoundedDomainCount();
        assertEquals(expected, actual, "Amount of bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCountTest() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & z : INT & y > z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCount2Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & x = y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCount3Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & x > 2*y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCount4Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & x >= 2*y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCount5Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & 2*y < x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierSemiBoundedDomainCount6Test() throws FeatureCreationException {
        String pred = "x : INTEGER & y : NATURAL & 2*y <= x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getIdentifierSemiBoundedDomainCount();
        assertEquals(expected, actual, "Amount of semi-bounded domains does not match");
    }

    @Test
    public void identifierUnboundedDomainCountTest() throws FeatureCreationException {
        String pred = "x : NAT1 & y : INTEGER & z : INT";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getIdentifierUnboundedDomainCount();
        assertEquals(expected, actual, "Amount of unbounded domains does not match");
    }

    @Test
    public void powerSetCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : POW1(POW(NAT)) & z : POW1(NAT)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getPowerSetCount();
        assertEquals(expected, actual, "Amount of power sets does not match");
    }

    @Test
    public void powerSetMaxDepthCountTest() throws FeatureCreationException {
        String pred = "x : NAT & y : POW(POW1(POW(NAT))) & z : POW(POW1(NAT))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));


        int expected = 3;
        int actual = data.getMaxPowDepth();
        assertEquals(expected, actual, "Power set max depth does not match");
    }

    @Test
    public void powerSetMaxDepthCount2Test() throws FeatureCreationException {
        String pred = "x : NAT & y : POW(POW1(POW(NAT))*POW1(INTEGER)) & z : POW(POW1(NAT))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));


        int expected = 3;
        int actual = data.getMaxPowDepth();
        assertEquals(expected, actual, "Power set max depth does not match");
    }

    @Test
    public void powerSetMaxDepthCount3Test() throws FeatureCreationException {
        String pred = "x : NAT & y : POW(POW1(NAT)*POW1(POW(INTEGER))) & z : POW(POW1(NAT))";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));


        int expected = 3;
        int actual = data.getMaxPowDepth();
        assertEquals(expected, actual, "Power set max depth does not match");
    }

    @Test
    public void powerSetHigherOrderTest() throws FeatureCreationException {
        String pred = "x : NAT & y : POW1(POW(NAT)) & z : POW1(NAT)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getPowerSetHigherOrderCounts();
        assertEquals(expected, actual, "Amount of power set stacks does not match");
    }

    @Test
    public void powerSetHigherOrder2Test() throws FeatureCreationException {
        String pred = "x : NAT & y : POW1(NAT*POW(NAT)) & z : POW1(NAT)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getPowerSetHigherOrderCounts();
        assertEquals(expected, actual, "Amount of power set stacks does not match");
    }

    @Test
    public void setCardCountTest() throws FeatureCreationException {
        String pred = "card(NAT) > 3";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetCardCount();
        assertEquals(expected, actual, "Cardinality count does not match");
    }

    @Test
    public void setUnionCountTest() throws FeatureCreationException {
        String pred = "x \\/ y = z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetUnionCount();
        assertEquals(expected, actual, "Amount of set unions does not match");
    }

    @Test
    public void setUnionCount2Test() throws FeatureCreationException {
        String pred = "x \\/ y \\/ a = z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getSetUnionCount();
        assertEquals(expected, actual, "Amount of set unions does not match");
    }

    @Test
    public void setUnionCount3Test() throws FeatureCreationException {
        String pred = "x = union({a,b,c,d})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetUnionCount();
        assertEquals(expected, actual, "Amount of set unions does not match");
    }

    @Test
    public void setUnionCount4Test() throws FeatureCreationException {
        String pred = "x = UNION(z).(card(z)=3 & z<:NAT | z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetUnionCount();
        assertEquals(expected, actual, "Amount of set unions does not match");
    }

    @Test
    public void setIntersecCountTest() throws FeatureCreationException {
        String pred = "x /\\ y = z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetIntersectCount();
        assertEquals(expected, actual, "Amount of set intersections does not match");
    }

    @Test
    public void setIntersecCount2Test() throws FeatureCreationException {
        String pred = "x /\\ y /\\ a = z";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getSetIntersectCount();
        assertEquals(expected, actual, "Amount of set intersections does not match");
    }

    @Test
    public void setIntersecCount3Test() throws FeatureCreationException {
        String pred = "x = inter({a,b,c,d})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetIntersectCount();
        assertEquals(expected, actual, "Amount of set intersections does not match");
    }

    @Test
    public void setIntersecCount4Test() throws FeatureCreationException {
        String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetIntersectCount();
        assertEquals(expected, actual, "Amount of set intersections does not match");
    }

    @Test
    public void setGeneralUnionCountTest() throws FeatureCreationException {
        String pred = "x = union({}) & y = union({a,b,c,d}) & z = union(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getSetGeneralUnionCount();
        assertEquals(expected, actual, "Amount of generalised unions does not match");
    }

    @Test
    public void setQualifiedUnionCountTest() throws FeatureCreationException {
        String pred = "x = UNION(z).(card(z)=3 & z<:NAT | z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetQuantifiedUnionCount();
        assertEquals(expected, actual, "Amount of generalised unions does not match");
    }

    @Test
    public void setGeneralIntersectCountTest() throws FeatureCreationException {
        String pred = "x = inter({}) & y = inter({a,b,c,d}) & z = inter(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getSetGeneralIntersectCount();
        assertEquals(expected, actual, "Amount of generalised intersections does not match");
    }

    @Test
    public void setQualifiedIntersectCountTest() throws FeatureCreationException {
        String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetQuantifiedIntersectCount();
        assertEquals(expected, actual, "Amount of generalised intersections does not match");
    }

    @Test
    public void setSubtractionCountTest() throws FeatureCreationException {
        String pred = "x = INTER(z).(card(z)=3 & z<:NAT | z) & y : FIN(NATURAL) & z = y\\x";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetQuantifiedIntersectCount();
        assertEquals(expected, actual, "Amount of generalised intersections does not match");
    }

    @Test
    public void setComprehensionCountTest() throws FeatureCreationException {
        String pred = "X = {x | y:NATURAL1 & x=y**2}";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSetComprehensionCount();
        assertEquals(expected, actual, "Amount of set comprehensions does not match");
    }

    @Test
    public void relCountTest() throws FeatureCreationException {
        String pred = "x : A<->B & y : POW(A<->B) & z : A<->(A<->B)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 4;
        int actual = data.getRelationCount();
        assertEquals(expected, actual, "Amount of relations does not match");
    }

    @Test
    public void totalRelCountTest() throws FeatureCreationException {
        String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getRelationTotalCount();
        assertEquals(expected, actual, "Amount of total relations does not match");
    }

    @Test
    public void surjRelCountTest() throws FeatureCreationException {
        String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getRelationSurjCount();
        assertEquals(expected, actual, "Amount of surjective relations does not match");
    }

    @Test
    public void totalSurjRelCountTest() throws FeatureCreationException {
        String pred = "x : A<<->B & y : POW(A<->>B) & z : A<->(A<<->>B)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRelationTotalSurjCount();
        assertEquals(expected, actual, "Amount of total surjective relations does not match");
    }

    @Test
    public void relImageCountTest() throws FeatureCreationException {
        String pred = "r : S<->T & S2 <: S & t = r[S2] & u = r[{1}]";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getRelationalImageCount();
        assertEquals(expected, actual, "Amount of relational images does not match");
    }

    @Test
    public void relInverseCountTest() throws FeatureCreationException {
        String pred = "x : A<<->B & y = x~";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRelationInverseCount();
        assertEquals(expected, actual, "Amount of inverse relations does not match");
    }

    @Test
    public void relOverrideCountTest() throws FeatureCreationException {
        String pred = "r1 : A<->B & r2 : A<->B & r = r1 <+ r2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRelationOverrideCount();
        assertEquals(expected, actual, "Amount of relational overrides does not match");
    }

    @Test
    public void relParallelProductCountTest() throws FeatureCreationException {
        String pred = "r1 : A<->B & r2 : A<->B & r = (r1 || r2)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRelationParallelProductCount();
        assertEquals(expected, actual, "Amount of relational parallel products does not match");
    }

    @Test
    public void relDirectProductCountTest() throws FeatureCreationException {
        String pred = "r1 : A<->B & r2 : A<->B & r = r1 >< r2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRelationDirectProductCount();
        assertEquals(expected, actual, "Amount of relational direct products does not match");
    }

    @Test
    public void domainCountTest() throws FeatureCreationException {
        String pred = "r1 : A<->B & r2 : B<->C & r = (r1;r2) & dom(r)=dom(r1) & ran(r)<:C";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getDomainCount();
        assertEquals(expected, actual, "Amount of dom() calls does not match");
    }

    @Test
    public void rangeCountTest() throws FeatureCreationException {
        String pred = "r1 : A<->B & r2 : B<->C & r = (r1;r2) & dom(r)=dom(r1) & ran(r)<:C";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRangeCount();
        assertEquals(expected, actual, "Amount of ran() calls does not match");
    }

    @Test
    public void relProj1CountTest() throws FeatureCreationException {
        String pred = "x = prj1(x,y) & y = prj2(x,y) & z = prj2(z,z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getProjection1Count();
        assertEquals(expected, actual, "Amount of first projections does not match");
    }

    @Test
    public void relProj2CountTest() throws FeatureCreationException {
        String pred = "x = prj1(x,y) & y = prj2(x,y) & z = prj2(z,z)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getProjection2Count();
        assertEquals(expected, actual, "Amount of second projections does not match");
    }

    @Test
    public void relForwardCompCountTest() throws FeatureCreationException {
        String pred = "x = {(1,2)} & y = {(2,3)} & z = (x ; y)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getForwardCompositionCount();
        assertEquals(expected, actual, "Amount of forward compositions does not match");
    }

    @Test
    public void relForwardCompCount2Test() throws FeatureCreationException {
        String pred = "x : S<->T & y : S<->T & z = (x ; y)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getForwardCompositionCount();
        assertEquals(expected, actual, "Amount of forward compositions does not match");
    }

    @Test
    public void domainRestrictionCountTest() throws FeatureCreationException {
        String pred = "r : S<->T & S2 <<: S & r2 = S2 <| r";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getDomainRestrictionCount();
        assertEquals(expected, actual, "Amount of domain restrictions does not match");
    }

    @Test
    public void domainSubtractionCountTest() throws FeatureCreationException {
        String pred = "r : S<->T & S2 <<: S & r2 = S2 <<| r";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getDomainSubtractionCount();
        assertEquals(expected, actual, "Amount of domain subtractions does not match");
    }

    @Test
    public void rangeRestrictionCountTest() throws FeatureCreationException {
        String pred = "r : S<->T & S2 <<: S & r2 = r |> S2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRangeRestrictionCount();
        assertEquals(expected, actual, "Amount of range restrictions does not match");
    }

    @Test
    public void rangeSubtractionCountTest() throws FeatureCreationException {
        String pred = "r : S<->T & S2 <<: S & r2 = r |>> S2";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRangeSubtractionCount();
        assertEquals(expected, actual, "Amount of range subtractions does not match");
    }

    @Test
    public void partialFunCountTest() throws FeatureCreationException {
        String pred = "f : A +-> B & g : B --> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunPartialCount();
        assertEquals(expected, actual, "Amount of partial functions does not match");
    }

    @Test
    public void totalFunCountTest() throws FeatureCreationException {
        String pred = "f : A +-> B & g : B --> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunTotalCount();
        assertEquals(expected, actual, "Amount of total functions does not match");
    }

    @Test
    public void partialInjFunCountTest() throws FeatureCreationException {
        String pred = "f : A >-> B & g : B >+> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunPartialInjCount();
        assertEquals(expected, actual, "Amount of partial injective functions does not match");
    }

    @Test
    public void totalInjFunCountTest() throws FeatureCreationException {
        String pred = "f : A >-> B & g : B >+> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunTotalInjCount();
        assertEquals(expected, actual, "Amount of total injective functions does not match");
    }

    @Test
    public void partialSurjFunCountTest() throws FeatureCreationException {
        String pred = "f : A -->> B & g : B +->> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunPartialSurjCount();
        assertEquals(expected, actual, "Amount of partial surjective functions does not match");
    }

    @Test
    public void totalSurjFunCountTest() throws FeatureCreationException {
        String pred = "f : A -->> B & g : B +->> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunTotalSurjCount();
        assertEquals(expected, actual, "Amount of total surjective functions does not match");
    }

    @Test
    public void partialBijFunCountTest() throws FeatureCreationException {
        String pred = "f : A >->> B & g : B >+>> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunPartialBijCount();
        assertEquals(expected, actual, "Amount of partial bijective functions does not match");
    }

    @Test
    public void totalBijFunCountTest() throws FeatureCreationException {
        String pred = "f : A >->> B & g : B >+>> A";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFunTotalBijCount();
        assertEquals(expected, actual, "Amount of total bijective functions does not match");
    }

    @Test
    public void funApplicationCountTest() throws FeatureCreationException {
        String pred = "f : A >->> B & g : B >+>> A & x = f(1) & y = f(2)*f(3)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getFunctionApplicationCount();
        assertEquals(expected, actual, "Amount of function applications does not match");
    }

    @Test
    public void lambdaFunCountTest() throws FeatureCreationException {
        String pred = "X = %x.(x:NATURAL|x**2)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getLambdaCount();
        assertEquals(expected, actual, "Amount of lambda abstractions does not match");
    }

    @Test
    public void seqCountTest() throws FeatureCreationException {
        String pred = "x = [1,2,3]";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSeqCount();
        assertEquals(expected, actual, "Amount of sequences does not match");
    }

    @Test
    public void seqCount2Test() throws FeatureCreationException {
        String pred = "x : seq({1,2,3})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSeqCount();
        assertEquals(expected, actual, "Amount of sequences does not match");
    }

    @Test
    public void seqCount3Test() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y : seq({1,2,3})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getSeqCount();
        assertEquals(expected, actual, "Amount of sequences does not match");
    }

    @Test
    public void seqCount4Test() throws FeatureCreationException {
        String pred = "x : iseq({1,2,3})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getSeqCount();
        assertEquals(expected, actual, "Amount of isequences does not match");
    }

    @Test
    public void seqCount5Test() throws FeatureCreationException {
        String pred = "x : iseq1({1,2,3}) & y : iseq({1,2,3})";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 2;
        int actual = data.getSeqCount();
        assertEquals(expected, actual, "Amount of isequences does not match");
    }

    @Test
    public void seqTailCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getTailCount();
        assertEquals(expected, actual, "Amount of sequence tails does not match");
    }

    @Test
    public void seqFirstCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFirstCount();
        assertEquals(expected, actual, "Amount of sequence heads does not match");
    }

    @Test
    public void seqLastCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = tail(x) & first(x) = last(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getLastCount();
        assertEquals(expected, actual, "Amount of sequence lasts does not match");
    }

    @Test
    public void seqFrontCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = front(x) & first(x) = last(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFrontCount();
        assertEquals(expected, actual, "Amount of sequence fronts does not match");
    }

    @Test
    public void seqInsertFrontCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = x <- 4 & z = 0 -> y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFrontInsertionCount();
        assertEquals(expected, actual, "Amount of sequence front insertions does not match");
    }

    @Test
    public void seqInsertTailCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = x <- 4 & z = 0 -> y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getTailInsertionCount();
        assertEquals(expected, actual, "Amount of sequence tail insertions does not match");
    }

    @Test
    public void seqRestrictFrontCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = x \\|/ 4 & z = y /|\\ 3";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getFrontRestrictionCount();
        assertEquals(expected, actual, "Amount of sequence front restrictions does not match");
    }

    @Test
    public void seqRestrictTailCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = x \\|/ 4 & z = y /|\\ 3";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getTailRestrictionCount();
        assertEquals(expected, actual, "Amount of sequence tail restrictions does not match");
    }

    @Test
    public void seqReverseCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & x /= rev(x)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getRevCount();
        assertEquals(expected, actual, "Amount of sequence reversions does not match");
    }

    @Test
    public void seqPermutationCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = perm(x) & first(x) /= first(y)";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getPermCount();
        assertEquals(expected, actual, "Amount of sequence permutations does not match");
    }

    @Test
    public void seqConcatCountTest() throws FeatureCreationException {
        String pred = "x : seq1({1,2,3}) & y = x^x & z = y^x^y";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 3;
        int actual = data.getConcatCount();
        assertEquals(expected, actual, "Amount of sequence concatenations does not match");
    }

    @Test
    public void seqGeneralConcatCountTest() throws FeatureCreationException {
        String pred = "S=seq(1..20) & x:S & y:S & z:S & u = conc([x,y,z])";
        BAstFeatureData data = BAstFeatureCollector.collect(BPredicate.of(pred));

        int expected = 1;
        int actual = data.getGeneralConcatCount();
        assertEquals(expected, actual, "Amount of general sequence concatenations does not match");
    }


}
