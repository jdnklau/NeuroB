package neurob.tests.features;

import static org.junit.Assert.*;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.be4.classicalb.core.parser.node.*;
import neurob.core.features.util.ArithmeticExpressionCheck;
import org.junit.Test;

/**
 * @author Jannik Dunkelau
 */
public class ArithmeticExpressionCheckTest {

	private Node getAST(String pred) throws BCompoundException {
		Start start = BParser.parse(BParser.PREDICATE_PREFIX + " 0 <" + pred);
		return ((ALessPredicate)
				((APredicateParseUnit) start.getPParseUnit())
						.getPredicate())
				.getRight();
	}

	@Test
	public void positiveTest1() throws BCompoundException {
		String pred = "x+y";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void positiveTest2() throws BCompoundException {
		String pred = "x+y-4";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void positiveTest3() throws BCompoundException {
		String pred = "18**19";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void positiveTest4() throws BCompoundException {
		String pred = "18**x";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void positiveTest5() throws BCompoundException {
		String pred = "72+18+19*44**2/114-(-1)";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void positiveTest6() throws BCompoundException {
		String pred = "18**x$0";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		assertTrue("simple arithmetic expression "+pred+" un-detected", check.isSimpleArithmetic());
	}

	@Test
	public void idCountTest1() throws BCompoundException {
		String pred = "x+y";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		int expected = 2;
		int actual = check.getIdCount();

		assertEquals("Amount of identifiers does not match", expected, actual);
	}

	@Test
	public void idCountPrimedTest1() throws BCompoundException {
		String pred = "x+y$0";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		int expected = 2;
		int actual = check.getIdCount();

		assertEquals("Amount of identifiers does not match", expected, actual);
	}

	@Test
	public void idCountTest2() throws BCompoundException {
		String pred = "x+y-4";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		int expected = 2;
		int actual = check.getIdCount();

		assertEquals("Amount of identifiers does not match", expected, actual);
	}

	@Test
	public void idCountTest3() throws BCompoundException {
		String pred = "(-4)";
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(getAST(pred));
		int expected = 0;
		int actual = check.getIdCount();

		assertEquals("Amount of identifiers does not match", expected, actual);
	}

	@Test
	public void negativeTest1() throws BCompoundException {
		String pred = "x:y";
		Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
		assertFalse("simple arithmetic expression "+pred+" falsely detected", check.isSimpleArithmetic());
	}

	@Test
	public void negativeTest2() throws BCompoundException {
		String pred = "x:POW(y)";
		Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
		assertFalse("simple arithmetic expression "+pred+" falsely detected", check.isSimpleArithmetic());
	}

	@Test
	public void negativeTest3() throws BCompoundException {
		String pred = "x:NAT*NAT";
		Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
		assertFalse("simple arithmetic expression "+pred+" falsely detected", check.isSimpleArithmetic());
	}

	@Test
	public void negativeTest4() throws BCompoundException {
		String pred = "x:POW(NAT) & x < 0";
		Start ast = BParser.parse(BParser.PREDICATE_PREFIX + " " + pred);
		ArithmeticExpressionCheck check = new ArithmeticExpressionCheck(ast);
		assertFalse("simple arithmetic expression "+pred+" falsely detected", check.isSimpleArithmetic());
	}
}
