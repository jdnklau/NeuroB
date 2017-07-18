package neurob.core.features.interfaces;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.prob.statespace.StateSpace;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.image.recordreader.ImageRecordReader;

import neurob.core.nets.util.ImageNameLabelGenerator;
import neurob.exceptions.NeuroBException;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;

/**
 * Generates the features of a given source. The source is translated into a corresponding image,
 * to be used by a convolutional neural network
 * @author jannik
 * @see FeatureGenerator
 *
 */
public interface ConvolutionFeatures extends FeatureGenerator {

	@Override
	default void setStateSpace(StateSpace stateSpace){
		// do nothing
	}

	/**
	 *
	 * @return Number of channels the resulting image has
	 */
	public int getFeatureChannels();

	/**
	 *
	 * @return Resulting image's height
	 */
	public int getImageHeight();


	/**
	 *
	 * @return Resulting image's width
	 */
	public int getImageWidth();

	/**
	 * Generates the features of the given source and returns them as image,
	 * without adding it to the intern feature list.
	 *
	 * @param source
	 * @return The generated feature image
	 * @throws NeuroBException
	 * @see #addData(String)
	 * @see #getFeatureImages()
	 *
	 */
	public BufferedImage generateFeatureImage(String source) throws NeuroBException;

	/**
	 * Translates an image generated by this feature generator to a string representation of it.
	 * @param image Image to translate
	 * @return Feature vector as string, comma separated
	 */
	public String translateImageFeatureToString(BufferedImage image);

	/**
	 * Translates a generated feature vector (a comma separated string) into a feature image.
	 * <p>
	 * This is inverse to {@link #translateImageFeatureToString(BufferedImage)}
	 * @param featureString The comma separated feature string to translate
	 * @return Feature image
	 */
	public BufferedImage translateStringFeatureToImage(String featureString);


	/**
	 * Translates an image generated by this feature generator to an array.
	 * <p>
	 * The resulting Array has one dimension, sequentially listing each row of pixels of the image.
	 * If the image has more than one channel, the channels values for each pixel are grouped sequentially
	 * before the next pixel's values are listed.
	 *
	 * @param image
	 * @return
	 */
	public double[] translateImageFeatureToArray(BufferedImage image);


	/**
	 * Translates a generated feature array into a featue image.
	 * <p>
	 * This is inverse to {@link #translateImageFeatureToArray(BufferedImage)}.
	 *
	 * @param features
	 * @return
	 */
	public BufferedImage translateArrayFeatureToImage(double[] features);

	/**
	 * Displays a given {@link BufferedImage}.
	 * <p>
	 * Usefull to show the generated features.
	 * @param image
	 */
	default public void display(BufferedImage image){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		frame.getContentPane().add(new JLabel(new ImageIcon(image)));

		frame.setVisible(true);
	}

	@Override
	default RecordReader getRecordReader(Path trainingSet, int batchSize) throws IOException, InterruptedException {
		ImageRecordReader rr =
				new ImageRecordReader(getImageHeight(), getImageWidth(),
						getFeatureChannels(), new ImageNameLabelGenerator());
		FileSplit fileSplit = new FileSplit(trainingSet.toFile(),
				new String[]{"png", "PNG"}, new Random(123));
		rr.initialize(fileSplit);

		return rr;
	}

	@Override
	default DataNormalization getNewNormalizer(){
		return new NormalizerMinMaxScaler();
	}
}
