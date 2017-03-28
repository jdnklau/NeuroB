package neurob.tests.training.generators;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import neurob.core.features.PredicateImages;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingImageGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateTrainingDataGeneratorTests {

	@Test
	public void PredicateImageNameTest() {
		PredicateTrainingImageGenerator gen = new PredicateTrainingImageGenerator(null, null);
		
		TrainingData td = new TrainingData(null, new double[]{1., 2.});
		
		String expected = "tmp/1.0,2.0/0_nosource.gif";
		String actual = gen.generateTargetFilePath(td.getSource(), 
				Paths.get("tmp/").resolve(td.getLabelString()))
				.toString();
		
		assertEquals("Generated image file name is incorrect.", expected, actual);
	}
	
	@Test
	public void PredicateImageNameByCreationTest() throws IOException, NeuroBException{
		Path target = Paths.get("tmp/1.0,2.0/0_nosource.gif"); // where the file should be located afterwards
		Files.deleteIfExists(target);
		
		PredicateTrainingImageGenerator gen = new PredicateTrainingImageGenerator(new PredicateImages(1), null);		
		List<TrainingData> trainingData = new ArrayList<>();
		trainingData.add(new TrainingData(new double[]{1.}, new double[]{1., 2.}));
		
		gen.writeTrainingDataToDirectory(trainingData, Paths.get("tmp"));
		
		assertTrue("Training image was not correctly created.", Files.exists(target));
		
		Files.deleteIfExists(target);
	}

}
