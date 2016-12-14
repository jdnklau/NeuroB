package neurob.core.features.interfaces;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.prob.statespace.StateSpace;
import neurob.exceptions.NeuroBException;

/**
 * Generates the features of a given predicate. The predicate is translated into a corresponding image,
 * to be used by a convolutional neural network
 * @author jannik
 * @see FeatureGenerator
 *
 */
public interface ConvolutionFeatures extends FeatureGenerator {
	
	/**
	 * Generates the features of the given predicate and returns them as image,
	 * without adding it to the intern feature list.
	 * 
	 * @param predicate
	 * @return The generated feature string
	 * @throws NeuroBException
	 * @see #addData(String)
	 * @see #getFeatureImages()
	 * 
	 */
	public BufferedImage generateFeatureImage(String predicate) throws NeuroBException;
	
	/**
	 * Access the features generated by this class.
	 * <br> The features are stored in a list; implementation of this list is up to 
	 * the individual implementing subclasses of this interface.
	 * <p>
	 * To generate the features, make use of the {@link #setData(StateSpace))} or {@link #addData(String)} methods.
	 * </p>
	 * @return A list of {@link BufferedImage}s, representing the features generated
	 * @see #addData(StateSpace)
	 * @see #addData(List)
	 * @see #addData(String)
	 */
	public List<BufferedImage> getFeatureImages();
	
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
}