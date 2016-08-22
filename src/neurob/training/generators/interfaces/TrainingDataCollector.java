/**
 * 
 */
package neurob.training.generators.interfaces;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public interface TrainingDataCollector {
	
	/**
	 * Collect the data from the given source file and save it to the target file.
	 * 
	 * Target file will be overridden if it already exists.
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	void collectTrainingData(Path source, Path target) throws IOException;
	
}
