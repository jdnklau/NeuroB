package neurob.training.generators;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.exceptions.NeuroBException;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;

public class PredicateTrainingImageGenerator extends PredicateTrainingDataGenerator {
	private AtomicInteger imageCounter; // counts number of images written
	private ConvolutionFeatures fg;
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingImageGenerator.class);

	public PredicateTrainingImageGenerator(ConvolutionFeatures fg, LabelGenerator lg) {
		super(fg, lg);
		this.fg = fg;
		
		preferedFileExtension = "gif";
		imageCounter = new AtomicInteger(0);
	}
	
	@Override
	public void writeTrainingDataToDirectory(List<TrainingData> trainingData, Path targetDir) throws NeuroBException {
		Path sourceFile;
		Path targetFile;
		
		for(TrainingData td : trainingData){
			/*
			 * Set up training data path.
			 * The target directory is appended by the label string,
			 * as the data set iterator for convolutional neural networks
			 * uses a parent path label generator to get the correct labelling. 
			 */
			sourceFile = td.getSource();
			targetFile = generateTargetFilePath(sourceFile, targetDir.resolve(td.getLabelString()));
			
			createTrainingImage(td, targetFile);
		}
	}
	
	private void createTrainingImage(TrainingData td, Path targetFile) {
		BufferedImage img = fg.translateArrayFeatureToImage(td.getFeatures());
		
		try {
			Files.createDirectories(targetFile.getParent());
			ImageIO.write(img, "gif", targetFile.toFile());
		} catch (IOException e) {
			log.error("Unable to write {}.", targetFile, e);
		}
	}

	@Override
	public Path generateTargetFilePath(Path sourceFile, Path targetDir) {
		String sourceFileName = sourceFile.getFileName().toString();
		
		int p = sourceFileName.lastIndexOf(".");
		String strippedSourceFileName = (p >=0) ? sourceFileName.substring(0, p) : sourceFileName;
		
		String targetFileName = imageCounter.getAndIncrement()
				+ "_" + strippedSourceFileName + "." + getPreferredFileExtension(); 
		
		return targetDir.resolve(targetFileName);
	}

}
