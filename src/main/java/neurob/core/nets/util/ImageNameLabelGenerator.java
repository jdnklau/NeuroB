package neurob.core.nets.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;

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
		String image = imagePath.getFileName().toString();
		String labels = image.split("[\\._]")[1]; // indexes: 0 is id, 1 is label string, 2 is gif
		
		return new Text(labels);
	}

}
