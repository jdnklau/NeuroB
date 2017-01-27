import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import neurob.core.NeuroB;
import neurob.core.features.PredicateFeatures;
import neurob.core.nets.NeuroBNet;
import neurob.core.nets.predefined.OldModels;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");
	private static NeuroB[] nbs;
	private static Path dir;
	private static HashMap<String, ArrayList<String>> ops;
	private static Path excludefile;
	// Count visited directories
	private static int seen=0, present=0, created=0;

	public static void main(String[] args) {
		// set up options hash map
		ops = new HashMap<String, ArrayList<String>>();		
		
		// collect command line options
		if(args.length >= 2){
			ArrayList<String> values = null;
			for(int i = 1; i<args.length; ++i){
				if(args[i].charAt(0) == '-'){
					// get option name
					String opt = args[i].substring(1);
					values = new ArrayList<String>();
					ops.put(opt, values);
				}
				else if(values != null){
					values.add(args[i]);
				}
				else {
					System.out.println("Unknown argument "+args[i]);
					return;
				}
				
			}
		}
		
		NeuroBCli.parseCommandLineOptions(ops);
		
		// extract command
		String cmd;
		if(args.length < 1){ // no commands
			cmd = "help"; // default
		}
		else {
			cmd = args[0];
		}
		
		// execute command
		
		if(cmd.equals("help")){
			String help =
					  "Call with the following arguments, where -net <net> indicates the neural net to be used (see below):\n"
					
					+ "trainingset -dir <directory> [-net <net>] [-excludefile <excludefile]\n"
					+ "\tGenerate training data from the mch files found in <directory>, but ignore those listed in <excludefile>\n"
					
					+ "trainingset -file <filename> [-net <net>]\n"
					+ "\tGenerate training data from a specific file. \n"
					
					+ "trainingset -analyse -dir <directory> [-net <net>]\n"
					+ "\tAnalyse the generated training data in <directory>\n"
					+ "\t<net> specifies the label format in use; specifically whether it is regression data or not\n"
					
					+ "trainingset -analyse -file <file> [-net <net>]\n"
					+ "\tAnalyse the generated training data in <file>\n"
					+ "\t<net> specifies the label format in use; specifically whether it is regression data or not\n"

					+ "trainingset -csvsplit -file <file> -first <first> -second <second> -ratio <ratio>\n"
					+ "\tSplit a given CSV file into <first> and <second>, both being CSV files again\n"
					+ "\tFor this, the given <ratio> is used, a number in the interval [0,1]\n"
					
					+ "trainnet -train <trainingfile> -test <testfile> [-net <net>]\n"
					+ "\tTrains a neural net with the given <trainingfile> and evaluates the training step on the given <testfile>\n"
					+ "\tBoth files being csv files generated with this tool\n"
					
					+ "trainnet -file <file> -n <number> [-net <net>]\n"
					+ "\tTrains <number> neural networks of type <net>, each with a different seed to begin with\n"
					
					+ "libraryIODef -dir <directory>\n"
					+ "\tDistributes the LibraryIO.def file in <directory>\n"

					+ "exclude [-excludefile <excludefile>] -source <toexcludes>\n"
					+ "\tSets <toexclude> (path to either directory or file) onto the specified <excludefile>, if not already present\n"
					+ "\t<toexcludes> can be a list of multiple paths to files or directories, separated by a blank space\n"
					
					+ "\nDefault values:\n"
					+ "- if -net <net> is not set, it defaults to 'psp' net\n"
					+ "- if -excludefile <excludefile> is not set, it defaults to default.excludes"
					+ "\t* if -excludefile none is set, no exclusions are made"
					
					+ "\nNets:\n"
					+ "The implemented nets you can access via the cli are\n"
					+ "\tprob - ProB only prediction (default)\n"
					+ "\tpsp - Predicate Solver Prediction\n"
					+ "\tpss - Predicate Solver Selection\n"
					+ "\tkodkod - KodKod only prediction\n"
					+ "\tprobcp - ProB only prediction using Code Portfolios\n"
					+ "\tpspcp - Predicate Solver Prediction using Code Portfolios\n"
					+ "\tpsscp - Predicate Solver Selection using Code Portfolios\n"
					+ "\tkodkodcp - KodKod only prediction using Code Portfolios\n"
					+ "\t\tNote: Code Portfolio models support usage of the -size argument.\n"
					+ "\t\t-size S, where S is the length of the image's sides, defaulting to 64."
					
					;
			
			System.out.println(help);
							  
		}
		// Generate training data
		else if(cmd.equals("trainingset")){
			// analyse training set
			if(ops.containsKey("analyse")){
				buildNet();
				
				if(ops.containsKey("dir")){
					analyseTrainingSet(dir, nbs[0].getNeuroBNet().getTrainingSetGenerator());
				}
				else if(ops.containsKey("file")){
					Path csv = Paths.get(ops.get("file").get(0));
					analyseTrainingSetCSV(csv, nbs[0].getNeuroBNet().getTrainingSetGenerator());
				}
				else{
					System.out.println("trainingset -analyse: Missing parameter, either -dir or -file");
				}
				
				
			}
			else if(ops.containsKey("csvsplit")){
				if(ops.containsKey("file")){
					if(ops.containsKey("first") && ops.containsKey("second") && ops.containsKey("ratio")){
						Path csv = Paths.get(ops.get("file").get(0));
						Path first = Paths.get(ops.get("first").get(0));
						Path second = Paths.get(ops.get("second").get(0));
						double ratio = Double.parseDouble(ops.get("ratio").get(0));
						csvsplit(csv, first, second, ratio);
						
					}
					else {
						System.out.println("trainingset -csvsplit: Missing at least one of those parameters: -first, -second, -ratio");
					}
				}
			}
			else if(ops.containsKey("dir")){// generate training set
				buildNet();
				trainingSetGeneration(dir);
			}
			// generate single nbtrain file
			else if(ops.containsKey("file")){
					buildNet();
					Path sourcefile = Paths.get(ops.get("file").get(0));
					singleTrainingDataGeneration(sourcefile);
				}
			// nope
			else {
				System.out.println("trainingset: missing at least -dir parameter");
			}
		}
		else if(cmd.equals("trainnet")){
			if(ops.containsKey("train")){
				if(ops.containsKey("test")){
					if(ops.containsKey("n")){
						int num = Integer.parseInt(ops.get("n").get(0));
						buildNets(num);
					} else {
						buildNet();
					}
					Path trainfile = Paths.get(ops.get("train").get(0));
					Path testfile = Paths.get(ops.get("test").get(0));
					trainNet(trainfile, testfile);
				}
				else {
					System.out.println("trainnet: missing -test parameter");
				}
			}
			// nope
			else {
				System.out.println("trainnet: missing -train parameter");
			}
		}
		else if(cmd.equals("trainmultiplenets")){
		}
		// distribute library file
		else if(cmd.equals("libraryIODef")){
			if(ops.containsKey("dir")){
				distribute(dir);
			}
			// nope
			else {
				System.out.println("libraryIODef: missing -dir parameter");
			}
		}
		// handle excludes
		else if(cmd.equals("exclude")){
			for(String s : ops.get("source")){
				exclude(excludefile, Paths.get(s));
			}
		}
		// unknown command
		else {
			System.out.println("Unknown command: "+cmd);
			System.out.println("Use help to show a list of available commands");
		}
		
		System.exit(0); // ensure that all ProBCli processes are closed after everything is done.
	}
	
	private static void buildNet(){
		buildNets(1);
	}
	
	private static void buildNets(int num){
		// Net to use
		NeuroBNet[] nets = new NeuroBNet[num];
		nbs = new NeuroB[num];
		// get net type
		String net = "prob";
		if(ops.containsKey("net")){
			net = ops.get("net").get(0);
		}
		// get size for code portfolio
		int size = 64;
		if(ops.containsKey("size")){
			size = Integer.parseInt(ops.get("size").get(0));
		}
		// set up nets
		for(int i=0; i<num; i++){
			if(net.equals("prob")){
				nets[i] = OldModels.getProBPredictionNet(i);
			} else if(net.equals("kodkod")){
				nets[i] = OldModels.getKodKodPredictionNet(i);
			} else if(net.equals("pss")){
				nets[i] = OldModels.getPredicateSolverSelectionNet(i);
			} else if(net.equals("psp")){
				nets[i] = OldModels.getPredicateSolverPredictionNet(i);
			
			} else if(net.equals("probcp")){
				nets[i] = OldModels.getProBPredictionWithCodePortfolioNet(i, size);
			} else if(net.equals("kodkodcp")){
				nets[i] = OldModels.getKodKodPredictionWithCodePortfolioNet(i, size);
			} else if(net.equals("psscp")){
				nets[i] = OldModels.getPredicateSolverSelectionWithCodePortfolioNet(i, size);
			} else if(net.equals("pspcp")){
				nets[i] = OldModels.getPredicateSolverPredictionWithCodePortfolioNet(i, size);
				
			} else {
				nets[i] = OldModels.getPredicateSolverPredictionNet(i);
				System.out.println("Net "+net+" is not known; defaulting to psp.");
			}
			
			nbs[i] = new NeuroB(nets[i]);
		}
	}

	private static void parseCommandLineOptions(HashMap<String, ArrayList<String>> ops) {
		
		// directory path
		if(ops.containsKey("dir")){
			dir = Paths.get(ops.get("dir").get(0));
		}
		
		// exclude file
		if(ops.containsKey("excludefile")){
			if(ops.get("excludefile").get(0).equals("none")){
				excludefile = null;
			}
			else {
				excludefile = Paths.get(ops.get("excludefile").get(0));
			}
		}
		else {
			// default
			excludefile = Paths.get("default.excludes");
		}
		
	}

	private static void distribute(Path directory){
		// Reset counters
		seen=0;
		present=0;
		created=0;
		
		try (Stream<Path> stream = Files.walk(directory)) {
	        stream.forEach(entry -> {
	        	// check if directory or not; recursion if so
	            if (Files.isDirectory(entry)) {
	            	seen++;
	            	Path newLibraryIOPath = entry.resolve("LibraryIO.def");
	            	try{
	            		Files.copy(libraryIOpath, newLibraryIOPath);
	            		System.out.println("Created: "+newLibraryIOPath);
	            		created++;
	            	} catch (IOException e){
	            		present++;
	            	}
	            }
	            
	        });
	        
	        System.out.println("LibraryIO.def was already present in "+present+"/"+seen+" directories.");
	        System.out.println("LibraryIO.def was created in "+created+"/"+seen+" directories.");
	        System.out.println("Directories without LibraryIO.def: "+ (seen-created-present));
	    }
		catch (IOException e){
			System.out.println("Could not access directory "+directory+": "+e.getMessage());
		}
	}
	
	private static void singleTrainingDataGeneration(Path source){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new PredicateFeatures(), new SolverClassificationGenerator(true, true, true));
		
		String fileName = source.getFileName().toString();
		Path target = Paths.get("training_data/single_file_generation/").resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
		tsg.generateTrainingDataFile(source, target);
	}
	
	private static void trainingSetGeneration(Path sourceDir){
		Path targetDir = Paths.get("training_data/");
		
		for(NeuroB nb : nbs){
			nb.generateTrainingSet(sourceDir, targetDir, excludefile);
		}
	}
	
	private static void analyseTrainingSet(Path dir, TrainingSetGenerator tsg) {
		try {
			tsg.logTrainingSetAnalysis(dir);
		} catch (IOException e) {
			System.out.println("Could not access target directory "+dir);
			e.printStackTrace();
		}
	}
	
	private static void analyseTrainingSetCSV(Path csv, TrainingSetGenerator tsg) {
		try {
			tsg.logTrainingCSVAnalysis(csv);
		} catch (IOException e) {
			System.out.println("Could not access target file "+csv);
			e.printStackTrace();
		}
	}
	
	private static void csvsplit(Path csv, Path first, Path second, double ratio){
		
		try {
			TrainingSetGenerator.splitCSV(csv, first, second, ratio, true);
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}
	
	private static void exclude(Path excludefile, Path excl) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(new PredicateFeatures(), new SolverClassificationGenerator(true, true, true));
		tsg.exclude(excludefile, excl);
		
	}
	
	private static void trainNet(Path traincsv, Path testcsv){
		for(NeuroB nb : nbs){
			try {
				nb.train(traincsv, testcsv);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
