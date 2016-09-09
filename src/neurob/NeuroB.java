package neurob;

import java.nio.file.Path;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.nd4j.linalg.dataset.DataSet;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;
import neurob.core.nets.interfaces.NeuroBNet;

public class NeuroB {
	private BParser bparser;
	private FeatureCollector featureCollector;
	private FeatureData fd;
	private NeuroBNet nbn;

	public NeuroB(NeuroBNet neuroBNet) {
		// set up parser
		bparser = new BParser();
		// set up feature handling
		featureCollector = new FeatureCollector();
		fd = new FeatureData();
		// link neural net
		nbn = neuroBNet;
		
	}
	
	public void processPredicate(String pred){
		try {
			// get AST from predicate
			System.out.println(pred);
			Start ast = bparser.parse(pred, false);
			
			// get features
			ast.apply(featureCollector);
			fd = featureCollector.getFeatureData();
			
			// print results
			System.out.println(fd);
			
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Trains the neural net with .nbtrain files found in the source directory.
	 * 
	 * The directory will be searched resursively
	 * @param sourceDirectory
	 */
	public void train(Path sourceDirectory){
		// set up training data
		RecordReader recordReader = new CSVRecordReader(0,",");
		
		DataSet ds;
	}

}
