package neurob.training;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.features.interfaces.FeatureGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.TrainingDataGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.DumpData;
import neurob.training.generators.util.TrainingData;

/**
 * Class to translate data dumps to real training sets.
 * @author jannik
 *
 */
public class DataDumpTranslator {
	
	private TrainingDataGenerator generator;
	// logger
	private static final Logger log = LoggerFactory.getLogger(DataDumpTranslator.class);
	
	/**
	 * Creates a DataDump translator, translating any data dump files to a format
	 * the generator would have created in the first place.
	 * @param generator
	 */
	public DataDumpTranslator(TrainingDataGenerator generator) {
		this.generator = generator;
	}
	
	/**
	 * Walks over the {@code source} directory recursively and tries to translate each
	 * {@code .*dump} file.
	 * <p>
	 * The generated training set will be placed in the {@code target} directory,
	 * as if it was created by the {@link TrainingSetGenerator} directly.
	 * @param source Source directory of all the data dump files
	 * @param target Target directory to write the training set to
	 */
	public void translateDumpDirectory(Path source, Path target){
		log.info("Translating data dumps from {} to {}", source, target);
		try(Stream<Path> stream = Files.walk(source)){
			/*
			 * Check if found Path object is a file
			 * Check if it is a .*dump file
			 * translate it
			 */
			stream
				.parallel()
				.filter(p->Files.isRegularFile(p))
				.filter(p->p.toString().endsWith("dump"))
				.forEach(p->translateDumpFile(p, target));
			
		} catch (IOException e) {
			log.error("Could not access source directory {}.", source, e);
		}
	}
	
	/**
	 * Translates the given dump file into valid training data.
	 * @param sourceFile
	 * @param targetDir
	 */
	public void translateDumpFile(Path sourceFile, Path targetDir){
		log.info("\tTranslating dump file {}", sourceFile);
		List<TrainingData> data;
		try {
			data = collectTrainingDataFromDumpFile(sourceFile);
			generator.writeTrainingDataToDirectory(data, targetDir);
		} catch (NeuroBException | IOException e) {
			log.error("Could not correctly translate dump file {}.", sourceFile, e);
		}
	}

	/**
	 * Generates a list of training data from the given data dump file.
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 */
	public List<TrainingData> collectTrainingDataFromDumpFile(Path sourceFile) throws IOException {
		List<TrainingData> data = new ArrayList<>();
		FeatureGenerator fg = generator.getFeatureGenerator();
		
		try(Stream<String> lines = Files.lines(sourceFile)){
			lines
				.forEach(l -> {
					if(l.startsWith("#")){ // skip those lines
						if(l.startsWith("source:", 1)){
							try {
								fg.setSourceFile(Paths.get(l.substring(8)));
							} catch (NeuroBException e) {
								log.warn("Could not set source file {}", l.substring(8), e);
							}
						}
						// else do nothing
					}
					else {
						try {
							data.add(translateDataDumpEntry(l));
						} catch (NeuroBException e) {
							log.warn("Could not translate data from dump entry {}", l, e);
						}
					}
				});
		}
		
		return data;
	}
	
	/**
	 * Translates a sample from a data dump file to training data.
	 * @param dataDumpEntry
	 * @return
	 * @throws NeuroBException
	 */
	public TrainingData translateDataDumpEntry(String dataDumpEntry) throws NeuroBException{
		FeatureGenerator fg = generator.getFeatureGenerator();
		LabelGenerator lg = generator.getLabelGenerator();
		
		// access dump data from entry, then translate to training data
		DumpData dd = new DumpData(dataDumpEntry);
		double[] features = fg.generateFeatureArray(dd.getSource());
		double[] labels = lg.translateLabelling(dd); 
		
		return new TrainingData(features, labels, fg.getSourceFile(), dd.getSource());
	}
}
