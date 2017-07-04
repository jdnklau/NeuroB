package neurob.core.features;

import neurob.core.features.interfaces.RNNFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingSequenceCSVGenerator;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Jannik Dunkelau
 */
public class RawPredicateSequences implements RNNFeatures {
	private Path sourceFile = null;

	@Override
	public INDArray generateFeatureNDArray(String source) throws NeuroBException {
		return Nd4j.create(generateFeatureArray(source));
	}

	@Override
	public double[] generateFeatureArray(String source) throws NeuroBException {
		// each character is a feature
		return source.codePoints().asDoubleStream().toArray();
	}

	@Override
	public int getFeatureDimension() {
		return 1;
	}

	@Override
	public void setSourceFile(Path sourceFile) throws NeuroBException {
		this.sourceFile = sourceFile;
	}

	@Override
	public Path getSourceFile() {
		return sourceFile;
	}

	@Override
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg) {
		return new PredicateTrainingSequenceCSVGenerator(this, lg);
	}


}
