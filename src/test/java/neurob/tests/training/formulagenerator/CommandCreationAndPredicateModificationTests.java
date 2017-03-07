package neurob.tests.training.formulagenerator;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.util.FormulaGenerator;

public class CommandCreationAndPredicateModificationTests {
	private Api api;
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	
	@Inject
	public CommandCreationAndPredicateModificationTests() {
		api = Main.getInjector().getInstance(Api.class);
	}
	
	@Test
	public void classicalBCommandByMachineTypeTest() throws NeuroBException{
		String pred = "x>y & y>x";
		
		IBEvalElement evalElem = FormulaGenerator.generateBCommandByMachineType(MachineType.CLASSICALB, pred);
		
		assertEquals("Wrong object created!", ClassicalB.class, evalElem.getClass());
	}
	
	@Test
	public void eventBCommandByMachineTypeTest() throws NeuroBException{
		String pred = "x>y & y>x";
		
		IBEvalElement evalElem = FormulaGenerator.generateBCommandByMachineType(MachineType.EVENTB, pred);
		
		assertEquals("Wrong object created!", EventB.class, evalElem.getClass());
	}
	
//	@Test
//	public void primedPredicateTest() throws IOException, ModelTranslationError, NeuroBException {
//		String pred = "x>y & y>x";
//		StateSpace ss = api.b_load(formulaeGenTestFile.toString());
//		
//		String primedPred = FormulaGenerator.generatePrimedPredicate(ss, pred);
//		
//		assertEquals("Predicate not correctly primed.", "x'>y' & y'>x'");
//	}

}
