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
import neurob.core.features.FeatureData;
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
	
	@Override
	public int getNumberOfFeatures() {return FeatureData.featureCount;}
	@Override
	public int getNumberOfLabels() {return 3;}
	
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
		// StateSpace and main component
		StateSpace ss;
		AbstractElement mainComp;
		// For the formula and ProB command to use
		String formula; // the conjunction of invariants
		EventB f; // formula as EventB formula
		CbcSolveCommand cmd;
		String res = ""; // for target vector
		// For logs
		String solver;
		
		// Access source file and use different solvers
		solver = "ProB";
		try{
			logger.fine("\tLoading machine with "+solver+"...");
			ss = api.b_load(source.toString());
			mainComp = ss.getMainComponent();	// extract main component
			
			// generate ProB command: assume conjunct of invariants is a constrained problem
			PredicateCollector predc = new PredicateCollector(mainComp);
			formula = String.join(" & ", predc.getInvariants());
			
			// check if invariants are non-empty
			if(!formula.isEmpty()){
				f = new EventB(formula);
				// check with: ProB
				logger.info("\tSolving with "+solver+"...");
				cmd = new CbcSolveCommand(f);
				try {
					ss.execute(cmd);
					res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
				} catch(Exception e) {
					// catch block is intended to catch invariants where ProB encounters problems with
					logger.warning("\tAt "+formula+":\t"+e.getMessage());
					res += "0";
				}
			}
			else {
				logger.info("\tNo invariants found.");

				// kill state space
				ss.kill();
				
				return;
			}
			// kill state space
			ss.kill();
		} catch(Exception e) {
			logger.severe("\tCould not load machine:" + e.getMessage());
			return;
		}
		
		// - KodKod
		solver = "KodKod";
		res +=","; // set delimiter to concatenate new label data
		try{
			logger.fine("\tLoading machine with "+solver+"...");
			ss = api.b_load(source.toString(), useKodKod);
			
			// check
			logger.info("\tSolving with "+solver+"...");
			cmd = new CbcSolveCommand(f);
			try {
				ss.execute(cmd);
				res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
			} catch(Exception e) {
				// catch block is intended to catch invariants where ProB encounters problems with
				logger.warning("\tAt "+formula+":\t"+e.getMessage());
				res += "0";
			}
			// kill state space
			ss.kill();
		} catch(Exception e) {
			logger.severe("\tCould not load machine with "+solver+":" + e.getMessage());
			return;
		}
		
		// - SMT
		solver = "SMT";
		res +=","; // set delimiter to concatenate new label data
		try{
			logger.fine("\tLoading machine with "+solver+"...");
			ss = api.b_load(source.toString(), useSMT);
			
			// check
			logger.info("\tSolving with "+solver+"...");
			cmd = new CbcSolveCommand(f);
			try {
				ss.execute(cmd);
				res += (cmd.getValue().toString().substring(0,4).equals("TRUE")) ? 1 : 0; // TRUE => 1; FALSE => 0
			} catch(Exception e) {
				// catch block is intended to catch invariants where ProB encounters problems with
				logger.warning("\tAt "+formula+":\t"+e.getMessage());
				res += "0";
			}
			// kill state space
			ss.kill();
		} catch(Exception e) {
			logger.severe("\tCould not load machine with "+solver+":" + e.getMessage());
			return;
		}
		
		// open target file
		BufferedWriter out = Files.newBufferedWriter(target);
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
		
		out.close();		
		
	}


}
