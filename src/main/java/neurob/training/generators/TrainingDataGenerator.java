package neurob.training.generators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.util.MachineType;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;

public abstract class TrainingDataGenerator {
	// logger
	private static final Logger log = LoggerFactory.getLogger(TrainingDataGenerator.class);
	
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
	 * Generates the training data from the given source file into the given target directory.
	 * <p>
	 * Only takes .mch and .bcm files into account. Other files will produce no output.
	 * @param sourceFile File from which the training data will be collected
	 * @param targetDir Target directory in which the training data files will be created
	 * @throws NeuroBException
	 * @throws IOException 
	 */
	public void generateTrainingDataFromFile(Path sourceFile, Path targetDir) throws NeuroBException, IOException{
		// determine machine type
		MachineType mt = determineMachineType(sourceFile);
		if(mt == null){
			log.debug("Not a machine file. Skipping: {}", sourceFile);			
			return;
		}
		
		// over the state space, the training data will be created
		StateSpace ss = loadStateSpace(sourceFile, mt);
		
		List<TrainingData> data = collectTrainingDataFromFile(sourceFile, ss);
		ss.kill(); // state space no longer of need
		
		if(data.isEmpty()){
			// no training data generated. leaving method
			log.info("\tNo training data created");
			return;
		}

		writeTrainingDataToDirectory(data, targetDir);
	}
	
	/**
	 * Collects a list of {@link TrainingData} over the given file or StateSpace.
	 * <p>
	 * What actually is used depends on the {@link TrainingDataGenerator} in use.
	 * @param sourceFile The file that is loaded into the state space
	 * @param stateSpace StateSpace loaded from source file
	 * @return
	 * @throws NeuroBException
	 */
	public abstract List<TrainingData> collectTrainingDataFromFile(Path sourceFile, StateSpace stateSpace) throws NeuroBException;
	
	/**
	 * Writes the training data collected to training data files in the given target directory.
	 * @param trainingData List of {@link TrainingData} instances, that are preferably collected via the same source file
	 * @param targetDir
	 * @throws NeuroBException
	 */
	public abstract void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir)
			throws NeuroBException;
	
	/**
	 * Creates a path to locate the a target file to be created in.
	 * <p>
	 * For {@code path/to/source/file} as source file and {@code target/directory/} as target directory,
	 * the file will be created in {@code target/directory/path/to/source/file.ext} with {@code ext} being
	 * the {@link #getPreferredFileExtension() preferred file extension}. 
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
	
	/**
	 * Determines the machine type based of the file extension of the source file.
	 * <p>
	 * If the source file is neither a .bcm or .mch file, {@code null} will be returned.
	 * @param sourceFile
	 * @return Type of the machine, or null if source file is not a B machine
	 */
	protected MachineType determineMachineType(Path sourceFile){
		String fileName = sourceFile.getFileName().toString();
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		if(ext.equals("bcm")){
			log.debug("EventB machine detected: {}", sourceFile);
			return MachineType.EVENTB;
		} else if(ext.equals("mch")) {
			log.debug("ClassicalB machine detected: {}", sourceFile);
			return MachineType.CLASSICALB;
		} else {
			return null;
		}
	}

	public FeatureGenerator getFeatureGenerator(){return fg;}
	public LabelGenerator getLabelGenerator(){return lg;}
	
	/**
	 * 
	 * @return The preferred file extension to use on the generated training data files; <b>without</b> leading '.' (dot). 
	 */
	public String getPreferredFileExtension(){ return preferedFileExtension;}
	
	/**
	 * Checks necessity of file creation.
	 * <p>
	 * If the target file already exists and is newer than the source file,
	 * then the data should be up to date (probably approximately correct).
	 * @param source
	 * @param target
	 * @return
	 * @throws IOException 
	 */
	protected boolean isTargetFileUpToDate(Path source, Path target) throws IOException{
		if(Files.exists(target, LinkOption.NOFOLLOW_LINKS)){
			if(Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS)
					.compareTo(Files.getLastModifiedTime(target, LinkOption.NOFOLLOW_LINKS))
				<= 0){ // last edit source file <= last edit target file -> nothing to do here
				return true;
			}
		}
		return true;
	}
	
}
