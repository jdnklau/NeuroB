package neurob.core.features;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

import de.be4.classicalb.core.parser.BParser;
import neurob.core.features.interfaces.PredicateASTFeatures;
import neurob.core.features.util.ClassicalBTheoryFeatureData;
import neurob.core.features.util.TheoryFeatureData;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;

public class TheoryFeatures implements PredicateASTFeatures {
	
	public static final int featureDimension = 17; // Dimension of feature vectors
	private ArrayList<String> features; // The stored features
	private MachineType machineType;
	private BParser bParser;
	private Path sourceFile;
	
	
	public TheoryFeatures() {
		this(MachineType.CLASSICALB);
	}
	
	public TheoryFeatures(Path machineFile) throws NeuroBException{
		reset();
		setMachine(machineFile);
	}
	
	public TheoryFeatures(MachineType mt){
		reset();
		machineType = mt;
	}

	@Override
	public void reset(){
		sourceFile = null;
		features = new ArrayList<String>();
		bParser = new BParser();
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
			machineType = MachineType.CLASSICALB;
		} else if(fileName.endsWith(".bcm")){
			machineType = MachineType.EVENTB;
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
			switch(machineType){
			case CLASSICALB:
			default: // defaulting to classical b
				pfd = new ClassicalBTheoryFeatureData(predicate, bParser);
			}
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
	public void addData(String predicate) throws NeuroBException {
		features.add(generateFeatureString(predicate));
	}

	@Override
	public List<String> getFeatureStrings() {
		return features;
	}
	
	@Override
	public int getFeatureDimension() {
		return featureDimension;
	}

}
