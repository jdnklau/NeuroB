package neurob.core.features;

import java.io.IOException;
import java.nio.file.Path;

import org.nd4j.linalg.api.ndarray.INDArray;

import de.be4.classicalb.core.parser.BParser;
import neurob.core.features.interfaces.PredicateASTFeatures;
import neurob.core.features.util.TheoryFeatureData;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.PredicateTrainingDataGenerator;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;

public class TheoryFeatures implements PredicateASTFeatures {
	
	public static final int featureDimension = 17; // Dimension of feature vectors
	private BParser bParser;
	private Path sourceFile;
	
	
	public TheoryFeatures() {
		sourceFile = null;
		bParser = new BParser();
	}
	
	public TheoryFeatures(Path machineFile) throws NeuroBException{
		this();
		setMachine(machineFile);
	}
	
	@Override
	public void setMachine(Path machineFile) throws NeuroBException{
		String fileName = machineFile.getFileName().toString();
		if(fileName.endsWith(".mch")) {
			try {
				bParser = new BParser(machineFile.toString());
				bParser.parseFile(machineFile.toFile(), false);
			} catch (IOException e) {
				throw new NeuroBException("Could not access source file "+machineFile, e);
			} catch (Exception e) {
				throw new NeuroBException("Could not parse source file "+machineFile, e);
			}
		} else if(fileName.endsWith(".bcm")){
			bParser = new BParser();
		}
		sourceFile = machineFile;
	}
	
	@Override
	public Path getSourceFile() {
		return sourceFile;
	}
	
	private TheoryFeatureData generatePredicateFeatureData(String predicate) throws NeuroBException{
		TheoryFeatureData pfd;
		try {
			pfd = new TheoryFeatureData(predicate, bParser);
		} catch (NeuroBException e) {
			throw new NeuroBException("Could not generate feature string from predicate: "+predicate, e);
		}
		return pfd;
	}
	
	@Override
	public INDArray generateFeatureNDArray(String predicate) throws NeuroBException {

		return generatePredicateFeatureData(predicate).toNDArray();
	}
	
	@Override
	public double[] generateFeatureArray(String predicate) throws NeuroBException {
		return generatePredicateFeatureData(predicate).toArray();
	}
	
	@Override
	public String generateFeatureString(String predicate) throws NeuroBException {
		return generatePredicateFeatureData(predicate).toString();
	}
	
	@Override
	public int getFeatureDimension() {
		return featureDimension;
	}
	
	@Override
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg) {
		return new PredicateTrainingDataGenerator(this, lg);
	}

}
