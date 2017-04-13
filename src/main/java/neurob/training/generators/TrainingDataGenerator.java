package neurob.training.generators;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

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
import neurob.training.TrainingSetAnalyser;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;

public abstract class TrainingDataGenerator {
	// logger
	private static final Logger log = LoggerFactory.getLogger(TrainingDataGenerator.class);
	
	protected final FeatureGenerator fg;
	protected final LabelGenerator lg;
	
	protected Api api;
	/**
	 * The preferred file extension to use for created training files.
	 */
	protected String preferredFileExtension;
	
	@Inject
	public TrainingDataGenerator(FeatureGenerator fg, LabelGenerator lg) {
		this.fg = fg;
		this.lg = lg;
		
		api = Main.getInjector().getInstance(Api.class);
		
		preferredFileExtension = "nbtrain";
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
		

		Path trainingDataLocation = generateTrainingDataPath(sourceFile, targetDir);
		log.info("Generating training data: {} -> {}...", sourceFile, trainingDataLocation);
		
		try {
			if(isTrainingDataUpToDate(sourceFile, trainingDataLocation)){
				log.info("\tTraining data {} seems already up to date, skipping.", trainingDataLocation);
				return;
			}
		} catch (IOException e) {
			throw new NeuroBException("Could not correctly access source file "+sourceFile
					+ " or training data location "+trainingDataLocation, e);
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
	public abstract List<TrainingData> collectTrainingDataFromFile(Path sourceFile, StateSpace stateSpace) 
			throws NeuroBException;
	
	/**
	 * Writes the training data collected to training data files in the given target directory.
	 * @param trainingData List of {@link TrainingData} instances, preferably collected via the same source file
	 * @param targetDir
	 * @throws NeuroBException
	 */
	public abstract void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir)
			throws NeuroBException;
	
	/**
	 * Creates a path locating the training data for the given file after creation.
	 * <p>
	 * This path may be one that is not existent, if the training data was not created in the first place.
	 * @param sourceFile
	 * @param targetDir
	 * @return
	 */
	public Path generateTrainingDataPath(Path sourceFile, Path targetDir){		
		String sourceFileName = sourceFile.getFileName().toString();
		String targetFileName = sourceFileName.substring(0, sourceFileName.lastIndexOf('.')+1)+preferredFileExtension;
		
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
		} catch(Exception e){
			throw new NeuroBException("Unexpected exception encountered: "+e.getMessage(), e);
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
	 * @return The preferred file extension to use on the generated training data files; <b>without</b> leading . (dot) 
	 */
	public String getPreferredFileExtension(){ return preferredFileExtension;}
	
	/**
	 * Checks necessity of file creation.
	 * <p>
	 * If training data for a file already exists and is newer than the source file,
	 * then the data should be up to date (probably approximately correct).
	 * @param source
	 * @param target
	 * @return true if training data exists, that is newer than the source file, false otherwise
	 * @throws IOException 
	 */
	protected boolean isTrainingDataUpToDate(Path source, Path target) throws IOException{
		if(Files.exists(target, LinkOption.NOFOLLOW_LINKS)){
			if(Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS)
					.compareTo(Files.getLastModifiedTime(target, LinkOption.NOFOLLOW_LINKS))
				<= 0){ // last edit source file <= last edit target file -> nothing to do here
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Splits the training data directory given into two parts.
	 * <p>
	 * A part approximately of the size of {@code ratio} will be written to {@code first}, the rest to {@code second}.
	 * E.g. if {@code ratio==0.6}, ~60% of data will be located in {@code first}.
	 * <p>
	 * The original data in {@code source} will not be altered nor deleted.
	 * 
	 * @param source Directory containing the training data to be split
	 * @param first Target directory to contain ~{@code ratio * source_data_count} samples
	 * @param second Target directory to contain ~{@code (1-ratio) * source_data_count} samples
	 * @param ratio A number in the interval [0,1]
	 * @param rng A random number generator to use for splitting
	 * @throws NeuroBException
	 */
	abstract public void splitTrainingData(Path source, Path first, Path second, double ratio, Random rng)
			throws NeuroBException;
	
	/**
	 * Trims the training data after analysing it, and saves the result into a given directory.
	 * <p>
	 * The original training data remains unchanged.
	 * 
	 * @param source Directory containing the training data to be trimmed
	 * @param target Directory to contain the trimmed data
	 * @throws NeuroBException
	 */
	abstract public void trimTrainingData(Path source, Path target) throws NeuroBException;
	
	/**
	 * Analyses the training set located in the source directory.
	 * @param source
	 * @return
	 * @throws NeuroBException
	 */
	public TrainingAnalysisData analyseTrainingSet(Path source) throws NeuroBException{
		TrainingAnalysisData data = getAnalysisData();
		
		try(Stream<Path> files = Files.walk(source)){
			files
				.filter(Files::isRegularFile)
				.filter(p->p.toString().endsWith("."+preferredFileExtension))
				.forEach(file->analyseTrainingFile(file, data));
		} catch (IOException e) {
			new NeuroBException("Could not analyse training set in "+source+" correctly.", e);
		}
		
		data.evaluateAllSamples();
		return data;
	}
	
	/**
	 * @return A {@link TrainingAnalysisData} object suitable for analysis training data generated by this generator.
	 */
	protected TrainingAnalysisData getAnalysisData(){
		return TrainingSetAnalyser.getAnalysisTypeByProblem(lg);
	}
	
	/**
	 * Analyses the training file and adds its data to the analysis data object.
	 * @param file
	 * @param analysisData
	 * @return Reference to the analysisData
	 */
	abstract protected TrainingAnalysisData analyseTrainingFile(Path file, TrainingAnalysisData analysisData);
	
	/**
	 * Takes a sample generated by this generator and returns the labelling assigned to it.
	 * <p>
	 * For generators, that write samples linewise to files, the sample input should be a line of said files.
	 * <br>
	 * For generators writing samples as files to directories, the sample input should be a path string, locating the
	 * file.
	 * <p>
	 * However, this depends on implementing class.
	 * @param sample Either line sample from file, or file name 
	 * @return The assigned labelling of the training data
	 */
	abstract public double[] labellingFromSample(String sample);
	
}
