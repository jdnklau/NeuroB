package neurob.tests.training;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Test;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;

public class DataCollectorTests {
	private Api api;
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
//	private final Path formulaeGenNBTrain = Paths.get("src/test/resources/training/formulae_generation.nbtrain");
	
	@Inject
	public DataCollectorTests() {
		api = Main.getInjector().getInstance(Api.class);
	}
	
	@Test
	public void extendedGuardFormulaeGeneration() throws IOException, ModelTranslationError{
		StateSpace ss = api.b_load(formulaeGenTestFile.toString());
		
		PredicateCollector pc = new PredicateCollector(ss);
		ArrayList<String> formulae = FormulaGenerator.extendedGuardFormulae(pc);
		
		assertEquals("Not enough formulae created", 49, formulae.size());
		
		ss.kill();
	}
	
	@Test
	public void extendedGuardFormulaeGenerationWithInfiniteDomains() throws IOException, ModelTranslationError{
		StateSpace ss = api.b_load(formulaeGenTestFile.toString());
		
		PredicateCollector pc = new PredicateCollector(ss);
		ArrayList<String> formulae = FormulaGenerator.extendedGuardFomulaeWithInfiniteDomains(pc);
		
		assertEquals("Not enough formulae created", 49, formulae.size());
		
		ss.kill();
	}
}
