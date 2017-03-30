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
		
		preferedFileExtension = "gif";
		imageCounter = 0;
	}
	
	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		Path sourceFile;
		Path targetFile;
		imageCounter = 0; // set to zero as we probably are looking at a new file
		
		log.info("Writing training images...");
		int c = 0;
		for(TrainingData td : trainingData){
			/*
			 * Set up training data path.
			 * The target directory is appended by the label string,
			 * as the data set iterator for convolutional neural networks
			 * uses a parent path label generator to get the correct labelling. 
			 */
			sourceFile = td.getSource();
			targetFile = generateTargetFilePath(sourceFile, targetDir.resolve(td.getLabelString(lg.getProblemType())));
			
			if(createTrainingImage(td, targetFile)){
				c++;
				/*
				 * count created images for final log output.
				 * Can not use difference of imageCounter as parallelism may influence
				 * the imageCounter, thus using private variable.
				 */
			}
		}
		log.info("\tDone: {} training images.", c);
	}
	
	private boolean createTrainingImage(TrainingData td, Path targetFile) {
		BufferedImage img = fg.translateArrayFeatureToImage(td.getFeatures());
		
		try {
			Files.createDirectories(targetFile.getParent());
			log.debug("\tWriting image {}", targetFile);
			ImageIO.write(img, "gif", targetFile.toFile());
			return true;
		} catch (IOException e) {
			log.error("\tUnable to write {}.", targetFile, e);
		}
		return false;
	}

	@Override
	public Path generateTargetFilePath(Path sourceFile, Path targetDir) {
		// get source file name without file extension
		String sourceFileName = sourceFile.getFileName().toString();
		int p = sourceFileName.lastIndexOf(".");
		String strippedSourceName = (p >=0) ? sourceFileName.substring(0, p) : sourceFileName;
		
		
		return targetDir.resolve( strippedSourceName + "_" + (imageCounter++) + "." + getPreferredFileExtension() );
	}

}
