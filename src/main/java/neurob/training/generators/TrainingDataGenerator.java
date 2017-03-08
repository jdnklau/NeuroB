package neurob.training.generators;

import java.io.IOException;
import java.nio.file.Path;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;

public abstract class TrainingDataGenerator {
	protected FeatureGenerator fg;
	protected LabelGenerator lg;
	
	protected Api api;
	/**
	 * The prefered file extension to use for created training files.
	 */
	protected String preferedFileExtension;
	
	@Inject
	public TrainingDataGenerator(FeatureGenerator fg, LabelGenerator lg) {
		this.fg = fg;
		this.lg = lg;
		
		api = Main.getInjector().getInstance(Api.class);
		
		preferedFileExtension = "nbtrain";
	}
	
	/**
	 * Generates training data from the given source into the target file.
	 * @param sourceFile
	 * @param targetDir
	 * @throws NeuroBException
	 */
	public abstract void collectTrainingDataFromFile(Path sourceFile, Path targetDir) throws NeuroBException;
	
	/**
	 * Creates a path to locate the a target file to be created in.
	 * <p>
	 * For {@code path/to/source/file} as source file and {@code target/directory/} as target directory,
	 * the file will be created in {@code target/directory/path/to/source/file.ext} with {@code ext} being
	 * the {@link #getPreferedFileExtension() prefered file extension}. 
	 * This keeps the data generated dereferencable wrt the source file.
	 * @param sourceFile
	 * @param targetDir
	 * @return
	 */
	public Path generateTargetFilePath(Path sourceFile, Path targetDir){		
		String sourceFileName = sourceFile.getFileName().toString();
		String targetFileName = sourceFileName.substring(0, sourceFileName.lastIndexOf('.')+1)+preferedFileExtension;
		
		return targetDir.resolve(sourceFile.getParent()).resolve(targetFileName);
	}

	/**
	 * Loads a state space by machine type.
	 * <p>
	 * As internally another method is called depending on whether the machine is
	 * e.g. a Classical B or EventB machine, the machine type has to be determined 
	 * beforehand.
	 * @param file
	 * @param mt
	 * @return
	 * @throws IOException
	 * @throws NeuroBException
	 */
	protected StateSpace loadStateSpace(Path file, MachineType mt) throws IOException, NeuroBException{
		try{
			switch(mt){
			case EVENTB:
				return api.eventb_load(file.toString());
			case CLASSICALB:
				return api.b_load(file.toString());
			default:
				throw new NeuroBException("Unexpected machine type given for state space loading: "+mt);
			}
		} catch(ModelTranslationError e){
			throw new NeuroBException("Unable to load state space due to model translation error.", e);
		}
	}

	public FeatureGenerator getFeatureGenerator(){return fg;}
	public LabelGenerator getLabelGenerator(){return lg;}
	
	/**
	 * 
	 * @return The preferred file extension to use on the generated training data files; <b>without</b> leading '.' (dot). 
	 */
	public String getPreferedFileExtension(){ return preferedFileExtension;}
	
}
