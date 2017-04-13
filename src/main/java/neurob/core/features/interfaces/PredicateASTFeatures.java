package neurob.core.features.interfaces;

import java.nio.file.Path;

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
}
