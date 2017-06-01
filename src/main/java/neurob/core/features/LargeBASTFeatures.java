package neurob.core.features;

import de.be4.classicalb.core.parser.BParser;
import neurob.core.features.interfaces.PredicateASTFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Jannik Dunkelau
 */
public class LargeBASTFeatures implements PredicateASTFeatures {
	private Path sourceFile;
	private BParser bParser;

	public LargeBASTFeatures(){
		sourceFile = null;
		bParser = new BParser();
	}

	@Override
	public void setMachine(Path machineFile) throws NeuroBException {
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
	public INDArray generateFeatureNDArray(String source) throws NeuroBException {
		return null;
	}

	@Override
	public double[] generateFeatureArray(String source) throws NeuroBException {
		return new double[0];
	}

	@Override
	public int getFeatureDimension() {
		return 0;
	}

	@Override
	public Path getSourceFile() {
		return sourceFile;
	}

	@Override
	public TrainingDataGenerator getTrainingDataGenerator(LabelGenerator lg) {
		return null;
	}
}
