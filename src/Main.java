
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import neurob.NeuroB;

/**
 * @author Jannik Dunkelau <jannik.dunkelau@hhu.de>
 *
 */
public class Main {

	public static void main(String[] args) {
		NeuroB nb = new NeuroB();
		
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
			nb.processPredicate(pred);
		}
		

	}

}
