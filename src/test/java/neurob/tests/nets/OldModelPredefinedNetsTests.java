package neurob.tests.nets;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import neurob.core.NeuroB;
import neurob.core.nets.NeuroBNet;
import neurob.core.nets.PredefinedNet;

public class OldModelPredefinedNetsTests {
	Path trainingDirectory = Paths.get("src/test/resources/training/");
	Path tmpDirectory = Paths.get("src/test/resources/tmp/training_data/");

	
	@Test
	public void KodKodPredictionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getKodKodPredictionNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
	@Test
	public void KodKodPredictionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getKodKodPredictionWithCodePortfolioNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
	@Test
	public void PredicateSolverPredictionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getPredicateSolverPredictionNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
	@Test
	public void PredicateSolverPredictionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getPredicateSolverPredictionWithCodePortfolioNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
	@Test
	public void PredicateSolverSelectionTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getPredicateSolverSelectionNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
	@Test
	public void PredicateSolverSelectionWithCodePortfolioTrainingTest() throws IOException, InterruptedException {
		NeuroBNet net = PredefinedNet.getPredicateSolverSelectionWithCodePortfolioNet(0);
		NeuroB nb = new NeuroB(net);
		
		Path data = tmpDirectory.resolve(net.getDataPathName()).resolve("data.csv");
		
		// make sure data.csv does not exist
		Files.deleteIfExists(data);
		
		nb.generateTrainingSet(trainingDirectory, tmpDirectory, null);
		
		assertTrue("Data Set not created", Files.exists(data));
		
		// delete directory
		Files.deleteIfExists(data);
	}
	
}
