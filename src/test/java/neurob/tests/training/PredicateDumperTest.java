package neurob.tests.training;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.Test;

import de.prob.scripting.ModelTranslationError;
import neurob.training.TrainingPredicateDumper;

public class PredicateDumperTest {
	private final Path formulaeGenTestFile = Paths.get("src/test/resources/training/formulae_generation.mch");
	private final Path formulaeGenPDump = Paths.get("src/test/resources/training/pdump/ClassicalB/src/test/resources/training/formulae_generation.pdump");
	
	@Test
	public void DumpFromFile() throws IOException, ModelTranslationError {
		TrainingPredicateDumper tpd = new TrainingPredicateDumper();
		
		Files.deleteIfExists(formulaeGenPDump);
		
		tpd.createPredicateDumpFromFile(formulaeGenTestFile, formulaeGenTestFile.getParent().resolve("pdump"));
		
		long entries = 0;
		try(Stream<String> stream = Files.lines(formulaeGenPDump)){
			entries = stream.count();
		}
		
		Files.deleteIfExists(formulaeGenPDump);
		
		assertEquals("Amount of dumped predicates does not match", 89, entries); 
	}

}
