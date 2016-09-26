package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.Main;
import de.prob.animator.command.CbcSolveCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
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
	private Logger logger = Logger.getLogger(TrainingSetGenerator.class.getName());

	@Inject
	public DefaultTrainingDataCollector() {
		fc = new FeatureCollector();
		
		api = Main.getInjector().getInstance(Api.class);
		
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
		ClassicalB f; // formula as EventB formula
		CbcSolveCommand cmd;
		String res = ""; // for target vector
		
		// Access source file
		try{
			logger.info("\tLoading machine...");
			ss = api.b_load(source.toString());
		}catch(Exception e) {
			logger.severe("\tCould not load machine:" + e.getMessage());
			return;
		}
		
		// Get invariants
		mainComp = ss.getMainComponent();	// extract main component
		PredicateCollector predc = new PredicateCollector(mainComp);
		formula = String.join(" & ", predc.getInvariants());
		
		// No invariants
		if(formula.isEmpty()){
			logger.info("\tNo invariants found.");
			// kill state space
			ss.kill();
			return;
		}
		
		// generate ProB command: assume conjunct of invariants is a constrained problem
		f = new ClassicalB(formula);
		
		// solve with different solvers:
		// ProB
		logger.info("\tSolving with ProB...");
		cmd = new CbcSolveCommand(f);
		res += evaluateCommandExecution(ss, cmd, formula);
		
		// KodKod
		res += ","; // Delimiter
		logger.info("\tSolving with KodKod...");
		cmd = new CbcSolveCommand(f);
		ss.execute(new SetPreferenceCommand("KODKOD", "true")); // turn KodKod on
		res += evaluateCommandExecution(ss, cmd, formula);
		ss.execute(new SetPreferenceCommand("KODKOD", "false")); // and turn it off again
		
		// SMT
		res += ","; // Delimiter
		logger.info("\tSolving with ProB/Z3...");
		cmd = new CbcSolveCommand(f);
		ss.execute(new SetPreferenceCommand("SMT_SUPPORTED_INTERPRETER", "true")); // turn SMT on
		res += evaluateCommandExecution(ss, cmd, formula);
		ss.execute(new SetPreferenceCommand("SMT_SUPPORTED_INTERPRETER", "false")); // and turn it off again
		
		// close StateSpace
		ss.kill();
		
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
	
	private String evaluateCommandExecution(StateSpace ss, CbcSolveCommand cmd, String formula){
		String res = "0";
		try {
			ss.execute(cmd);
			
			// get value for result
			AbstractEvalResult cmdres = cmd.getValue();
			if(cmdres instanceof EvalResult){
				// could solve or disprove it
				String val = ((EvalResult) cmdres).getValue();
				if(val.equals("TRUE")){
					res = "1"; // solved
				}
				else if(val.equals("FALSE")){
					res = "-1"; // unsolvable
				}
				else {
					// This should logically not happen
					res = "0";
				}
			} else if(cmdres instanceof ComputationNotCompletedResult){
				// Could not solve nor disprove the predicate in question
				res = "0";
			} else {
				// durr?
				res = "0";
			}
		} catch(Exception e) {
			// catch block is intended to catch invariants where ProB encounters problems with
			logger.warning("\tAt "+formula+":\t"+e.getMessage());
			res = "0";
		}
		return res;
	}


}
