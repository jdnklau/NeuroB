package neurob.training.generators;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.inject.Inject;

import de.be4.classicalb.core.parser.BParser;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.be4.classicalb.core.parser.node.Start;
import de.prob.Main;
import de.prob.model.representation.AbstractElement;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import neurob.core.features.FeatureCollector;
import neurob.training.generators.helpers.PredicateCollector;
import neurob.training.generators.interfaces.TrainingDataCollector;

public class DefaultTrainingDataCollector implements TrainingDataCollector {
	private FeatureCollector fc;
	private Api api;
	private BParser bparser;

	@Inject
	public DefaultTrainingDataCollector() {
		fc = new FeatureCollector();
		bparser = new BParser();
		
		api = Main.getInjector().getInstance(Api.class);
	}

	@Override
	public void collectTrainingData(Path source, Path target) throws IOException, BException {
		// access source file
		Start ast = bparser.parseFile(source.toFile(), false);
		
//		ast.apply(fc);
//		BufferedWriter out = Files.newBufferedWriter(target);
//		out.write(fc.getFeatureData().toString());
//		out.close();
		
		StateSpace ss = api.b_load(source.toString());
		AbstractElement mainComp = ss.getMainComponent();
		
		// open target file
		BufferedWriter out = Files.newBufferedWriter(target);
		
		// assume invariants are constraint problems
		// get them and try to solve them
		PredicateCollector predc = new PredicateCollector(mainComp);
		for(String s : predc.getInvariants()){
			Start inv = BParser.parse("#PREDICATE "+s);
			inv.apply(fc);
			
			out.write(fc.getFeatureData().toString()+":"+s+"\n");
			out.flush();
			
		}
		
		out.close();
		ss.kill();
		
		
	}


}
