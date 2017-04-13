package neurob.core.nets.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;

/**
 * Extracts the labelling of an image file with naming convention {id}_{labels}.gif,
 * where {id} is an arbitrary number, and {labels} is a comma separated string of numbers.
 * @author jannik
 *
 */
public class ImageNameLabelGenerator implements PathLabelGenerator {

	private static final long serialVersionUID = 132266541864989542L;

	@Override
	public Writable getLabelForPath(String path) {
		Path imagePath = Paths.get(path);
		return labelForImage(imagePath);
	}

	@Override
	public Writable getLabelForPath(URI uri) {
		Path imagePath = Paths.get(uri);
		return labelForImage(imagePath);
	}

	private Writable labelForImage(Path imagePath) {
		return new Text(labelStringForImage(imagePath));
	}
	
	/**
	 * Returns the labels of an image as comam separated String.
	 * Here it is assumed the image name has the correct format.
	 * @param imagePath
	 * @return
	 */
	public static String labelStringForImage(Path imagePath){
		String image = imagePath.getFileName().toString();
		return image.split("[\\._]")[1]; // indexes: 0 is id, 1 is label string, 2 is gif
	}

}
