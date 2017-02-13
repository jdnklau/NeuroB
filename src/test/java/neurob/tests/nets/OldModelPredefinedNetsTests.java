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

		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);

		assertTrue("Data Set for training not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
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
