package neurob.tests.labels;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateEvaluator;

public class PredicateEvaluationTests {
	private final String pred = "x<y & y<x";
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	
	private Api api;
	private StateSpace ss;
	private IBEvalElement cmd;

	@Inject
	public PredicateEvaluationTests() throws IOException, ModelTranslationError, NeuroBException {
		api = Main.getInjector().getInstance(Api.class);
		
		ss = api.b_load(formulaeGenTestFile.toString());
		cmd = FormulaGenerator.generateBCommandByMachineType(ss, pred);
	}
	
	@Test
	public void ProBEvalTest() throws NeuroBException {
		boolean expected = false;
		boolean actual = PredicateEvaluator.isDecidableWithSolver(ss, SolverType.PROB, cmd);
		
		assertEquals("Unexpected decidability of predicate.", expected, actual);
	}
	
	@Test
	public void KodKodEvalTest() throws NeuroBException {
		boolean expected = false;
		boolean actual = PredicateEvaluator.isDecidableWithSolver(ss, SolverType.KODKOD, cmd);
		
		assertEquals("Unexpected decidability of predicate.", expected, actual);
	}
	
	@Test
	public void Z3EvalTest() throws NeuroBException {
		boolean expected = true;
		boolean actual = PredicateEvaluator.isDecidableWithSolver(ss, SolverType.Z3, cmd);
		
		assertEquals("Unexpected decidability of predicate.", expected, actual);
	}
	
	@Test
	public void ProBZ3EvalTest() throws NeuroBException {
		boolean expected = true;
		boolean actual = PredicateEvaluator.isDecidableWithSolver(ss, SolverType.SMT_SUPPORTED_INTERPRETER, cmd);
		
		assertEquals("Unexpected decidability of predicate.", expected, actual);
	}

}
