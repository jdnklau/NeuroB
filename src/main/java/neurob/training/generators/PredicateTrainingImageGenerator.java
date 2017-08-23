package neurob.training.generators;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.nets.util.ImageNameLabelGenerator;
import neurob.exceptions.NeuroBException;
import neurob.training.analysis.TrainingAnalysisData;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.util.TrainingData;
import neurob.training.splitting.TrainingSetSplitter;
import neurob.training.splitting.TrainingSetTrimmer;

public class PredicateTrainingImageGenerator extends PredicateTrainingDataGenerator {
	private int imageCounter; // counts number of images written
	private ConvolutionFeatures fg;
	private static final Logger log = LoggerFactory.getLogger(PredicateTrainingImageGenerator.class);

	public PredicateTrainingImageGenerator(ConvolutionFeatures fg, LabelGenerator lg) {
		super(fg, lg);
		this.fg = fg;

		preferredFileExtension = "png";
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
			ImageIO.write(img, "png", targetFile.toFile());
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
		if(sourceFile == null)
			sourceFile = Paths.get("null_source");
		return targetDir.resolve(sourceFile.toString()+".image_dir");
	}

	@Override
	public void splitTrainingData(Path source, Path first, Path second, double ratio, Random rng)
			throws NeuroBException {
		TrainingSetSplitter.splitFilewise(source, first, second, ratio, rng, "."+preferredFileExtension);
	}

	@Override
	protected TrainingAnalysisData analyseTrainingFile(Path file, TrainingAnalysisData analysisData) {
		// load features and labels
		String labelString = ImageNameLabelGenerator.labelStringForImage(file);

		BufferedImage img;
		try {
			img = ImageIO.read(file.toFile());
		} catch (IOException e) {
			log.error("Could not read {}", file, e);
			return analysisData;
		}

		// translate features
		double[] features = fg.translateImageFeatureToArray(img);
		double[] labels = Arrays.stream(labelString.split(",")).mapToDouble(Double::valueOf).toArray();

		analysisData.analyseSample(features, labels);

		return analysisData;
	}

	@Override
	public void trimTrainingData(Path source, Path target) throws NeuroBException {
		TrainingAnalysisData analysisData = analyseTrainingSet(source);
		TrainingSetTrimmer.trimFilewise(source, target, analysisData, this, "."+preferredFileExtension);
	}

	@Override
	public double[] labellingFromSample(String sample) {
		String labelStr = ImageNameLabelGenerator.labelStringForImage(Paths.get(sample));

		return Arrays.stream(labelStr.split(",")).mapToDouble(Double::valueOf).toArray();
	}
}
