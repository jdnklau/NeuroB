package neurob.tests.training;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Test;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.training.generators.SolverClassificationDataCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;
import neurob.training.generators.util.FormulaGenerator;
import neurob.training.generators.util.PredicateCollector;

public class DataCollectorTests {
	private Api api;
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	private final Path formulaeGenNBTrain = Paths.get("src/test/resources/training/formulae_generation.nbtrain");
	
	@Inject
	public DataCollectorTests() {
		api = Main.getInjector().getInstance(Api.class);
	}
	
	@Test
	public void extendedGuardFormulaeGeneration() throws IOException, ModelTranslationError{
		StateSpace ss = api.b_load(formulaeGenTestFile.toString());
		
		PredicateCollector pc = new PredicateCollector(ss.getMainComponent());
		ArrayList<String> formulae = FormulaGenerator.extendedGuardFormulas(pc);
		
		assertEquals("Not enough formulae created", 50, formulae.size());
		
		ss.kill();
	}
	
	/**
	 * Test if the collector runs properly or exits with an error
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws BException
	 */
	@Test
	public void solverClassification() throws IllegalStateException, IOException, BException{
		TrainingDataCollector tdc = new SolverClassificationDataCollector();
		tdc.collectTrainingData(formulaeGenTestFile, formulaeGenNBTrain);
		Files.deleteIfExists(formulaeGenNBTrain);
	}
}
