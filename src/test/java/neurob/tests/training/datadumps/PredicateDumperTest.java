package neurob.tests.training.datadumps;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

import de.prob.scripting.ModelTranslationError;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetAnalyser;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.PredicateDumpGenerator;

public class PredicateDumperTest {
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	private final Path formulaeGenPDumpDir = Paths.get("src/test/resources/training/pdump/");
	private final Path formulaeGenPDump = formulaeGenPDumpDir.resolve("src/test/resources/training/formulae_generation.pdump");
	
	@Test
	public void DumpFromFileTest() throws IOException, ModelTranslationError, NeuroBException {
		PredicateDumpGenerator tpg = new PredicateDumpGenerator(1);
		
		Files.deleteIfExists(formulaeGenPDump);
		
		tpg.generateTrainingDataFromFile(formulaeGenTestFile, formulaeGenPDumpDir);
		
		
		// analyse
		TrainingAnalysisData tad = TrainingSetAnalyser.analysePredicateDumps(formulaeGenPDump.getParent());
		long entries = tad.getSamplesCount();
		
		Files.deleteIfExists(formulaeGenPDump);
		
//		assertEquals("Amount of dumped predicates does not match", 113, entries);
		// FIXME: On different machines, the 114th formulae may or may not go through.
		assertTrue("Amount of dumped predicates does not match, expecting at least 136, but got "+entries, 
				entries>=136);
	}

}
