package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Logger;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.Main;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.domainobjects.EventB;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.FeatureCollector;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.helpers.PredicateCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class DefaultTrainingDataCollector implements TrainingDataCollector {
	private FeatureCollector fc;
	private Api api;
	private HashMap<String, String> useKodKod;
	private HashMap<String, String> useSMT;
	private Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());

	@Inject
	public DefaultTrainingDataCollector() {
		fc = new FeatureCollector();
		
		api = Main.getInjector().getInstance(Api.class);
		
		// set up solvers		
		useKodKod = new HashMap<String,String>();
		useKodKod.put("KODKOD", "true");
		
		useSMT = new HashMap<String,String>();
		useSMT.put("SMT", "true");
		
	}
	
	/**
	 * Set the logger to a different one
	 * @param l
	 */
	@Override
	public void setLogger(Logger l){
		logger = l;
	}

	@Override
	public void collectTrainingData(Path source, Path target) throws IOException, BException {
		// StateSpaces
		StateSpace ss, sskod, sssmt;
		
		// access source file
		try{
			ss = api.b_load(source.toString());
		} catch(Exception e) {
			logger.severe("\tCould not load machine:" + e.getMessage());
			return;
		}
		AbstractElement mainComp = ss.getMainComponent();
		
		// load source file to different solvers
		try{
			sskod = api.b_load(source.toString(), useKodKod);
		} catch(Exception e) {
			ss.kill();
			logger.severe("\tCould not load machine with KodKod:" + e.getMessage());
			return;
		}
		try{
			sssmt = api.b_load(source.toString(), useSMT);
		} catch(Exception e) {
			ss.kill();
			sskod.kill();
			logger.severe("\tCould not load machine with SMT:" + e.getMessage());
			return;
		}
		
		// open target file
		BufferedWriter out = Files.newBufferedWriter(target);
		
		// assume conjunct of invariants is a constrained problem
		PredicateCollector predc = new PredicateCollector(mainComp);
		String formula = String.join(" & ", predc.getInvariants());
		
		
		// set up command to send to ProB
		EventB f = new EventB(formula);
		CbcSolveCommand cmd;
		String res = ""; // for target vector
		
		try{
			// try different solvers
			// - default
			logger.info("\tSolving with ProB...");
			cmd = new CbcSolveCommand(f);
			ss.execute(cmd);
			res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
			// - KodKod
			logger.info("\tSolving with KodKod...");
			res += ","; // separate from previous result
			cmd = new CbcSolveCommand(f);
			sskod.execute(cmd);
			res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
			// - SMT
			logger.info("\tSolving with SMT...");
			res += ","; // separate from previous result
			cmd = new CbcSolveCommand(f);
			sssmt.execute(cmd);
			res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
			

			// write feature vector to stream
			logger.info("\tWriting training data...");
			Start inv = BParser.parse("#PREDICATE "+formula);
			inv.apply(fc);
			out.write(fc.getFeatureData().toString()); // feature vector
			// delimiter for target vector
			out.write(":");
			// write target vector
			out.write(res);
			
			// end line
			out.write("\n");
			out.flush();
		} catch(Exception e) {
			// catch block is intended to catch invariants where ProB encounters problems with
			logger.warning("\tAt "+formula+":\t"+e.getMessage());
		}
		
		out.close();
		ss.kill();
		sskod.kill();
		sssmt.kill();
		
		
	}


}
