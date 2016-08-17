
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import neurob.core.features.FeatureCollector;
import neurob.core.features.FeatureData;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class Main {

	public static void main(String[] args) {
		BParser p = new BParser();
		Start ast;
		FeatureCollector fc = new FeatureCollector();
		
		// get test predicates
		String testFileUri = "examples/basic_examples.txt";
		List<String> preds = new ArrayList<String>();
		try (Stream<String> stream = Files.lines(Paths.get(testFileUri))){
			stream.forEach(preds::add);
		} catch (IOException e1) {
			System.out.println("Could not access test file");
			e1.printStackTrace();
		}
		
		// get features of test predicates
		for(String pred : preds){
			try {
				// get AST from predicate
				System.out.println(pred);
				ast = p.parse(pred, false);
				
				// get features
				ast.apply(fc);
				
				
				// print results
				System.out.println(fc.getFeatureData());
				
			} catch (BException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

	}

}
