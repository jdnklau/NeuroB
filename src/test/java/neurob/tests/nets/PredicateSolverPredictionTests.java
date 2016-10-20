package neurob.tests.nets;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.datavec.api.records.reader.RecordReader;
import org.junit.Test;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import neurob.core.nets.PredicateSolverPredictionNet;
import neurob.core.nets.interfaces.NeuroBNet;

public class PredicateSolverPredictionTests {
	private NeuroBNet net;
	private final Path fakeDataFile = Paths.get("src/test/resources/small_fake_data.csv");
	
	public PredicateSolverPredictionTests() {
		net = new PredicateSolverPredictionNet().setSeed(0L).build();
	}
	
	@Test
	public void recordReaderAndIterator() throws IOException, InterruptedException{
		RecordReader rr = net.getRecordReader(fakeDataFile);
		DataSetIterator it = net.getDataSetIterator(rr);
		
		while(it.hasNext()){
			DataSet batch = it.next();
			net.fit(batch);
		}
	}
}
