package neurob.core.features.interfaces;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Jannik Dunkelau
 */
public interface RNNFeatures extends FeatureGenerator {

	/**
	 * Returns a record reader that is specifically intended for sequential data.
	 * <p>
	 *     It is assumed, that the data is in the form of "dataSet/%d.csv", with %d being an integer
	 *     from 0 to a maximum index n.
	 * </p>
	 * @param dataSet
	 * @param batchSize
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	default SequenceRecordReader getSequenceRecordReader(Path dataSet, int batchSize) throws IOException, InterruptedException{
		SequenceRecordReader recordReader = new CSVSequenceRecordReader(2, ",");

		// set up file split
		String format = dataSet.toString()+"/%d.csv";
		int maxIdx = 0;
		try(Stream<Path> files = Files.walk(dataSet)){
			// count how many csv files are present
			int count = (int) files.filter(s->s.toString().endsWith(".csv")).count();
			maxIdx = count-1; // 0 until count-1 are the indices
		}

		NumberedFileInputSplit split =
				new NumberedFileInputSplit(format, 0, maxIdx);

		recordReader.initialize(split);
		return recordReader;
	}

	@Override
	default RecordReader getRecordReader(Path trainingSet, int batchSize) throws IOException, InterruptedException{
		return getSequenceRecordReader(trainingSet, batchSize);
	}
}
