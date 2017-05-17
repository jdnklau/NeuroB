package neurob.core.nets;

import neurob.core.features.interfaces.RNNFeatures;
import neurob.core.util.ProblemType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Jannik Dunkelau
 */
public class NeuroBRecurrentNet extends NeuroBNet {

	public NeuroBRecurrentNet(RNNFeatures features, LabelGenerator labelling) {
		super(features, labelling);
	}

	public NeuroBRecurrentNet(MultiLayerNetwork model, RNNFeatures features, LabelGenerator labelling) {
		super(model, features, labelling);
	}

	public NeuroBRecurrentNet(int[] hiddenLayers, double learningRate, RNNFeatures features, LabelGenerator labelling) {
		super(hiddenLayers, learningRate, features, labelling);
	}

	public NeuroBRecurrentNet(int[] hiddenLayers, double learningRate, RNNFeatures features, LabelGenerator labelling, int seed) {
		super(hiddenLayers, learningRate, features, labelling, seed);
	}

	public NeuroBRecurrentNet(Path modelDirectory, RNNFeatures features, LabelGenerator labelling) throws NeuroBException {
		super(modelDirectory, features, labelling);
	}

	@Override
	public DataSetIterator getDataSetIterator(Path dataSet, int batchSize) throws IOException, InterruptedException {
		SequenceRecordReader featureReader = (SequenceRecordReader) features.getRecordReader(dataSet.resolve("features"), batchSize);
		SequenceRecordReader labelReader = (SequenceRecordReader) features.getRecordReader(dataSet.resolve("labels"), batchSize);

		return new SequenceRecordReaderDataSetIterator(featureReader, labelReader, batchSize,
				labelgen.getClassCount(), labelgen.getProblemType() == ProblemType.REGRESSION);
	}
}
