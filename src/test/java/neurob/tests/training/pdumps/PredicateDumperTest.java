package neurob.tests.training.pdumps;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.Test;

import de.prob.scripting.ModelTranslationError;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateDumpGenerator;

public class PredicateDumperTest {
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	private final Path formulaeGenPDump = Paths.get("src/test/resources/training/pdump/formulae_generation.pdump");
	
	@Test
	public void DumpFromFileTest() throws IOException, ModelTranslationError, NeuroBException {
		PredicateDumpGenerator tpg = new PredicateDumpGenerator(1);
		
		Files.deleteIfExists(formulaeGenPDump);
		
		tpg.collectTrainingDataFromFile(formulaeGenTestFile, formulaeGenPDump);
		
		long entries = 0;
		try(Stream<String> stream = Files.lines(formulaeGenPDump)){
			entries = stream.count();
		}
		
		Files.deleteIfExists(formulaeGenPDump);
		
//		assertEquals("Amount of dumped predicates does not match", 113, entries);
		// FIXME: On different machines, the 114th formulae may or may not go through.
		assertTrue("Amount of dumpef predicates does not match, expecting at least 136, but got "+entries, 
				entries==136||entries==137);
	}

}
