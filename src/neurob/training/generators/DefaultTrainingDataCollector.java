package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class DefaultTrainingDataCollector implements TrainingDataCollector {
	BParser bparse;
	FeatureCollector fc;

	public DefaultTrainingDataCollector() {
		bparse = new BParser();
		fc = new FeatureCollector();
	}

	@Override
	public void collectTrainingData(Path source, Path target) throws IOException {
		
		// get features
		try {
			System.out.println("Parsing "+source); // TODO: delete this line
			Start ast = bparse.parseFile(source.toFile(), false);
			ast.apply(fc);
			
			FeatureData fd = fc.getFeatureData();
			
			BufferedWriter out = Files.newBufferedWriter(target);
			out.write(fd.toString());
			out.close();
			
			
		} catch (BException e) {
			System.out.println("Could not parse "+source+": "+e.getMessage());
		}
		
	}


}
