package neurob.tests.labels;

import static org.junit.Assert.*;

import org.junit.Test;

import de.prob.animator.command.CbcSolveCommand.Solvers;
import neurob.core.util.SolverType;

public class SolverTypeTranslationTests {

	@Test
	public void ProBTest() {
		assertEquals("Non-matching solver type translation.", Solvers.PROB, SolverType.PROB.toCbcSolveCommandEnum());
	}

	@Test
	public void KodKodTest() {
		assertEquals("Non-matching solver type translation.", Solvers.KODKOD, SolverType.KODKOD.toCbcSolveCommandEnum());
	}

	@Test
	public void Z3Test() {
		assertEquals("Non-matching solver type translation.", Solvers.Z3, SolverType.Z3.toCbcSolveCommandEnum());
	}

	@Test
	public void SmtSupportedInterpreterTest() {
		assertEquals("Non-matching solver type translation.", Solvers.SMT_SUPPORTED_INTERPRETER, SolverType.SMT_SUPPORTED_INTERPRETER.toCbcSolveCommandEnum());
	}

}
