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
import neurob.core.features.TheoryFeatures;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingCSVGenerator;
import neurob.training.generators.PredicateTrainingImageGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateTrainingDataGeneratorTests {
	private final Path nosource = Paths.get("nosource.nsrc");

	@Test
	public void PredicateImageLocationTest() {
		PredicateTrainingImageGenerator gen = new PredicateTrainingImageGenerator(null, null);
		
		TrainingData td = new TrainingData(null, new double[]{1., 2.}, nosource);
		
		String expected = "tmp/nosource.nsrc.image_dir";
		String actual = gen.generateTrainingDataPath(td.getSource(), 
				Paths.get("tmp/"))
				.toString();
		
		assertEquals("Generated image file name is incorrect.", expected, actual);
	}
	
	@Test
	public void PredicateImageNameByCreationTest() throws IOException, NeuroBException{
		// where the file should be located afterwards
		Path target = Paths.get("src/test/resources/tmp/nosource.nsrc.image_dir/0_1,2.gif");
		Files.deleteIfExists(target);
		
		PredicateTrainingImageGenerator gen = new PredicateTrainingImageGenerator(
				new PredicateImages(1), 
				new SolverSelectionGenerator());		
		List<TrainingData> trainingData = new ArrayList<>();
		trainingData.add(new TrainingData(new double[]{1.}, new double[]{1., 2.}, nosource));
		
		gen.writeTrainingDataToDirectory(trainingData, Paths.get("src/test/resources/tmp/"));
		
		assertTrue("Training image was not correctly created.", Files.exists(target));
		
		Files.deleteIfExists(target);
	}
	
	@Test
	public void PredicateCSVHeaderTest() {
		PredicateTrainingCSVGenerator gen = new PredicateTrainingCSVGenerator(
				new TheoryFeatures(), new SolverSelectionGenerator());
		String expected = "Feature0,Feature1,Feature2,Feature3,Feature4,Feature5,Feature6,Feature7,Feature8,"
				+ "Feature9,Feature10,Feature11,Feature12,Feature13,Feature14,Feature15,Feature16,"
				+ "Label0";
		String actual = gen.getCSVHeader();
		
		assertEquals("CSV Headers do not match", expected, actual);
	}

}
