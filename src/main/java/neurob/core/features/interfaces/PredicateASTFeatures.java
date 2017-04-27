package neurob.core.features.interfaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;

import neurob.exceptions.NeuroBException;

public interface PredicateASTFeatures extends FeatureGenerator {
	
	/**
	 * Sets the machine file over which the predicates will be parsed.
	 * <p>
	 * This is mainly necessary due to the definitions in classical B, as their absence 
	 * results in unparseable predicates.
	 * @param machineFile
	 * @throws NeuroBException
	 */
	public void setMachine(Path machineFile) throws NeuroBException;
	
	@Override
	default void setSourceFile(Path sourceFile) throws NeuroBException {
		setMachine(sourceFile);
	}
	
	@Override
	default RecordReader getRecordReader(Path trainingSet, int batchSize)
			throws IOException, InterruptedException {
		RecordReader recordReader = new CSVRecordReader(1,","); // skip first line (header line)
		FileSplit fileSplit = new FileSplit(trainingSet.toFile(),
				new String[]{"csv", "CSV"}, new Random(123));
		recordReader.initialize(fileSplit);
		
		return recordReader;
	}
}
