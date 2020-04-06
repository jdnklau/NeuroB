package de.hhu.stups.neurob.core.features.predicates.util;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticExpressionCheckTest {
    private Node getAST(String pred) throws BCompoundException {
        Start start = BParser.parse(BParser.PREDICATE_PREFIX + " 0 <" + pred);
        return ((ALessPredicate)
                ((APredicateParseUnit) start.getPParseUnit())
                        .getPredicate())
                .getRight();
    }

    @Test
    public void shouldAcceptAdditionOfVariables() throws BCompoundException {
        String pred = "x+y";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldAcceptAdditionWithSubtraction() throws BCompoundException {
        String pred = "x+y-4";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldAcceptExponentiation() throws BCompoundException {
        String pred = "18**19";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldAcceptExponentiationWithVariable() throws BCompoundException {
        String pred = "18**x";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldAcceptMixOfAdditionMultiplicationAndExponentiation() throws BCompoundException {
        String pred = "72+18+19*44**2/114-(-1)";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldAcceptFormulaeWithDollarVariables() throws BCompoundException {
        String pred = "18**x$0";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        assertTrue(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " un-detected");
    }

    @Test
    public void shouldCountIdentifiersInArithmetic() throws BCompoundException {
        String pred = "x+y";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        int expected = 2;
        int actual = check.getIdCount();

        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void shouldCountPrimedIdentifiersInArithmetic() throws BCompoundException {
        String pred = "x+y$0";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        int expected = 2;
        int actual = check.getIdCount();

        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void shouldCountIdentifiersInArithmeticMixedWithNumbers() throws BCompoundException {
        String pred = "x+y-4";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        int expected = 2;
        int actual = check.getIdCount();

        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void shouldNotDetectAnyIdentifiers() throws BCompoundException {
        String pred = "(-4)";
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
        int expected = 0;
        int actual = check.getIdCount();

        assertEquals(expected, actual, "Amount of identifiers does not match");
    }

    @Test
    public void shouldNotAcceptSetMembership() throws BCompoundException {
        String pred = "x:y";
        Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
        assertFalse(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " falsely detected");
    }

    @Test
    public void shouldNotAcceptSetMembershipOfPowerSets() throws BCompoundException {
        String pred = "x:POW(y)";
        Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
        assertFalse(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " falsely detected");
    }

    @Test
    public void shoultNotAcceptSetMembershipOfCartesianProduct() throws BCompoundException {
        String pred = "x:NAT*NAT";
        Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
        assertFalse(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " falsely detected");
    }

    @Test
    public void shouldNotAcceptComparisons() throws BCompoundException {
        String pred = "x:POW(NAT) & x < 0";
        Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
        ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
        assertFalse(check.isSimpleArithmetic(), "simple arithmetic expression " + pred + " falsely detected");
    }
}
