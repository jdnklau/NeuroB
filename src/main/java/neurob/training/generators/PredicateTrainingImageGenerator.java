package neurob.training.generators;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateTrainingImageGenerator extends PredicateTrainingDataGenerator {
	private int imageCounter; // counts number of images written
	private ConvolutionFeatures fg;
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingImageGenerator.class);

	public PredicateTrainingImageGenerator(ConvolutionFeatures fg, LabelGenerator lg) {
		super(fg, lg);
		this.fg = fg;
		
		preferredFileExtension = "gif";
		imageCounter = 0;
	}
	
	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		Path sourceFile;
		Path targetFile;
		imageCounter = 0; // set to zero as we probably are looking at a new file
		
		log.info("Writing training images...");
		for(TrainingData td : trainingData){
			/*
			 * Set up training data path.
			 * The images will be located in a directory given by #generateTrainingDataPath
			 * and are named by this pattern: id_labelling.gif
			 * with id being a counter of images created thus far
			 * and labelling being the label string in CSV format.
			 */
			sourceFile = td.getSource();
			Path targetFileDir = generateTrainingDataPath(sourceFile, targetDir);
			targetFile = targetFileDir.resolve(
					imageCounter 
					+ "_" + td.getLabelString(lg.getProblemType()) +  "." + getPreferredFileExtension());
			
			createTrainingImage(td, targetFile);
		}
		log.info("\tDone: {} training images.", imageCounter);
	}
	
	private boolean createTrainingImage(TrainingData td, Path targetFile) {
		BufferedImage img = fg.translateArrayFeatureToImage(td.getFeatures());
		
		try {
			Files.createDirectories(targetFile.getParent());
			log.debug("\tWriting image {}", targetFile);
			ImageIO.write(img, "gif", targetFile.toFile());
			imageCounter++;
			return true;
		} catch (IOException e) {
			log.error("\tUnable to write {}.", targetFile, e);
		}
		return false;
	}

	@Override
	public Path generateTrainingDataPath(Path sourceFile, Path targetDir) {
		// get source file name without file extension
		return targetDir.resolve(sourceFile.toString()+".image_dir");
	}

}
