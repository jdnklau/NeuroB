/**
 * 
 */
package neurob.training.generators.interfaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import de.be4.classicalb.core.parser.exceptions.BException;

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
	 * @throws BException 
	 */
	void collectTrainingData(Path source, Path target) throws IOException, BException;
	
	/**
	 * Sets a Logger
	 * May be that no logger is supported; then does nothing
	 * @param l The Logger to be used
	 */
	default void setLogger(Logger l) {}
	
	/**
	 * Returns the number of features selected
	 * @return
	 * @see {@link #getNumberOfLabels()}
	 */
	int getNumberOfFeatures();
	
	/**
	 * Returns the number of labels a single training vector has 
	 * @return
	 * @see {@link #getNumberOfFeatures()}
	 */
	int getNumberOfLabels();
	
}
