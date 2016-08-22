import java.io.File;
import java.io.IOException;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;

public class TrainingSetGeneration {

	public static void main(String[] args) {
		BParser bpars = new BParser();
		String examplePath = "prob_examples/public_examples/B/";
		
		File mch = new File(examplePath+"EventB/Lift.mch");
		
		try {
			Start ast = bpars.parseFile(mch, false);
			
			System.out.println(ast);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
