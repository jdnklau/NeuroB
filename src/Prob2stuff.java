import java.io.IOException;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.Main;
import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.cli.ProBInstanceProvider;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import de.prob.scripting.Api;
import de.prob.statespace.*;

public class Prob2stuff {
	
	
	@Inject
	public Prob2stuff() {
		String testfilepath = "prob_examples/public_examples/B/ABCD/bookstore.mch";
		
		Api api = Main.getInjector().getInstance(Api.class);
		
		try {
			StateSpace s = api.b_load(testfilepath);

			
			EventB f = new EventB("a = 1 & b = 2*a");
			CbcSolveCommand c = new CbcSolveCommand(f);
			
			
			s.execute(c);
			System.out.println(c.getValue());
			
			
		} catch (IOException | BException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		Prob2stuff p = new Prob2stuff();
	}

}
