package neurob.tests.nets;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import neurob.core.NeuroB;
import neurob.core.nets.NeuroBNet;
import neurob.core.nets.predefined.OldModels;

public class OldModelPredefinedNetsTests {
	Path trainingDirectory = Paths.get("src/test/resources/training/");
	Path tmpDirectory = Paths.get("src/test/resources/tmp/training_data/");

	
	private void checkTrainingSetGeneration(NeuroBNet net)  throws IOException, InterruptedException {
		NeuroB nb = new NeuroB(net);

		Path traindata = tmpDirectory.resolve(net.getDataPathName()).resolve("train_data.csv");
		Path testdata = tmpDirectory.resolve(net.getDataPathName()).resolve("train_data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(traindata);
		Files.deleteIfExists(testdata);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);

		assertTrue("Training Data Set not created", Files.exists(traindata));
		assertTrue("Test Data Set not created", Files.exists(testdata));
		
		// delete directory
		Files.deleteIfExists(traindata);
		Files.deleteIfExists(testdata);
	}
	
	@Test
	public void ProBPredictionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getProBPredictionNet(0);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void ProBPredictionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getProBPredictionWithCodePortfolioNet(0,64);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void KodKodPredictionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getKodKodPredictionNet(0);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void KodKodPredictionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getKodKodPredictionWithCodePortfolioNet(0,64);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void PredicateSolverPredictionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getPredicateSolverPredictionNet(0);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void PredicateSolverPredictionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getPredicateSolverPredictionWithCodePortfolioNet(0,64);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void PredicateSolverSelectionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getPredicateSolverSelectionNet(0);
		checkTrainingSetGeneration(net);
	}
	
	@Test
	public void PredicateSolverSelectionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = OldModels.getPredicateSolverSelectionWithCodePortfolioNet(0,64);
		checkTrainingSetGeneration(net);
	}
	
}
