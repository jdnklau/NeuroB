import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import neurob.core.NeuroB;
import neurob.core.features.CodePortfolios;
import neurob.core.features.PredicateFeatures;
import neurob.core.nets.NeuroBNet;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");
	private static NeuroB nb;
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
					
					+ "trainingset -csvtranslate -file <csv> -dir <directory> [-net <net>]\n"
					+ "\tFor models with convolutional features, this translates the data.csv from training set generation into"
					+ "\ta directory structure in <directory>, that contains the classes as subdirectories and places all"
					+ "\tsamples as image files accordingly inside one of them"
					
					+ "trainnet -train <trainingfile> -test <testfile> [-net <net>]\n"
					+ "\tTrains a neural net with the given <trainingfile> and evaluates the training step on the given <testfile>\n"
					+ "\tBoth files being csv files generated with this tool\n"
					
					+ "trainnet -train <traindata> -test <testdata> [-hidden <layer_sizes +>] [-seed <seed +>] [-epochs <epochs +>] [-lr <learningrate +>] [-net <net>]\n"
					+ "\tTrains a neural networks model of type <net>\n"
					+ "\tThe model is trained on <traindata>, then evaluated on <testdata>\n"
					+ "\t-hidden determines the number and size of hidden layers; -hidden 256 128 128 would create 3 hidden layers with respective amount of neurons"
					+ "\t\tDefault: -hidden 512 256 128 128"
					+ "\tThe defaults for the other hyper parameters are seed: 0, epochs: 15, learningrate: 0.006"
					+ "\t\tNote: One can set multiple values for the hyper parameters seed, epochs, and lr, resulting in training each possible combination"
					+ "\t\t      so be carefull with how many you query"
					+ "\t\tExample: -seed 1 2 -lr 0.006 0.0007"
					
					+ "libraryIODef -dir <directory>\n"
					+ "\tDistributes the LibraryIO.def file in <directory>\n"

					+ "exclude [-excludefile <excludefile>] -source <toexcludes>\n"
					+ "\tSets <toexclude> (path to either directory or file) onto the specified <excludefile>, if not already present\n"
					+ "\t<toexcludes> can be a list of multiple paths to files or directories, separated by a blank space\n"
					
					+ "\nDefault values:\n"
					+ "- if -net <net> is not set, it defaults to 'prob' net\n"
					+ "- if -excludefile <excludefile> is not set, it defaults to default.excludes"
					+ "\t* if -excludefile none is set, no exclusions are made"
					
					+ "\nNets:\n"
					+ "The implemented nets you can access via the cli are\n"
					+ "\tprob - ProB only prediction (default)\n"
					//+ "\tpsp - Predicate Solver Prediction\n"
					+ "\tpss - Predicate Solver Selection\n"
					+ "\tkodkod - KodKod only prediction\n"
					+ "\tprobcp - ProB only prediction using Code Portfolios\n"
					//+ "\tpspcp - Predicate Solver Prediction using Code Portfolios\n"
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
					analyseTrainingSet(dir, nb.getNeuroBNet().getTrainingSetGenerator());
				}
				else if(ops.containsKey("file")){
					Path csv = Paths.get(ops.get("file").get(0));
					analyseTrainingSetCSV(csv, nb.getNeuroBNet().getTrainingSetGenerator());
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
			else if(ops.containsKey("csvtranslate")){
				if(ops.containsKey("file")){
					if(ops.containsKey("dir")){
						Path csv = Paths.get(ops.get("file").get(0));
						Path dir = Paths.get(ops.get("dir").get(0));
						buildNet();
						translateCsv(csv, dir);
						
					}
					else {
						System.out.println("trainingset -csvtranslate: Missing parameter: -dir");
					}
				}
				else {
					System.out.println("trainingset -csvtranslate: Missing parameter: -file");
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
		// trainnet -train <traindata> -test <testdata> [-seed <seed>+] [-epochs <epochs>+] [-lr <learningrate>+] [-net <net>]
		else if(cmd.equals("trainnet")){
			if(ops.containsKey("train")){
				if(ops.containsKey("test")){
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
	
	private static void translateCsv(Path csv, Path dir) {
		nb.getNeuroBNet().getTrainingSetGenerator().translateCSVToImages(csv, dir);
	}

	private static void buildNet(){
		buildNet(1);
	}
	
	private static void buildNet(int i){
		buildNet(i, 0.006, new int[]{200});
	}
	
	private static void buildNet(int seed, double learningrate, int[] hiddenLayers){
		NeuroBNet model;
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
//		int i = seed;
//		if(net.equals("prob")){
//			model = OldModels.getProBPredictionNet(i);
//		} else if(net.equals("kodkod")){
//			model = OldModels.getKodKodPredictionNet(i);
//		} else if(net.equals("pss")){
//			model = OldModels.getPredicateSolverSelectionNet(i);
//		} else if(net.equals("psp")){
//			model = OldModels.getPredicateSolverPredictionNet(i);
//		
//		} else if(net.equals("probcp")){
//			model = OldModels.getProBPredictionWithCodePortfolioNet(i, size);
//		} else if(net.equals("kodkodcp")){
//			model = OldModels.getKodKodPredictionWithCodePortfolioNet(i, size);
//		} else if(net.equals("psscp")){
//			model = OldModels.getPredicateSolverSelectionWithCodePortfolioNet(i, size);
//		} else if(net.equals("pspcp")){
//			model = OldModels.getPredicateSolverPredictionWithCodePortfolioNet(i, size);
//			
//		} else {
//			model = OldModels.getPredicateSolverPredictionNet(i);
//			System.out.println("Net "+net+" is not known; defaulting to psp.");
//		}
		//if(net.equals("prob")){
			model = new NeuroBNet(hiddenLayers, learningrate, new PredicateFeatures(), new SolverClassificationGenerator(true, false, false), seed);
		//} else 
		if(net.equals("kodkod")){
			model = new NeuroBNet(hiddenLayers, learningrate, new PredicateFeatures(), new SolverClassificationGenerator(false, true, false), seed);
		} else if(net.equals("pss")){
			model = new NeuroBNet(hiddenLayers, learningrate, new PredicateFeatures(), new SolverSelectionGenerator(), seed);
		}
		else if(net.equals("probcp")){
			model = new NeuroBNet(hiddenLayers, learningrate, new CodePortfolios(size), new SolverClassificationGenerator(true, false, false), seed);
		} else if(net.equals("kodkodcp")){
			model = new NeuroBNet(hiddenLayers, learningrate, new CodePortfolios(size), new SolverClassificationGenerator(false, true, false), seed);
		} else if(net.equals("psscp")){
			model = new NeuroBNet(hiddenLayers, learningrate, new CodePortfolios(size), new SolverSelectionGenerator(), seed);
		} 
	
		nb = new NeuroB(model);
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
		tsg.generateTrainingDataFromFile(source, target);
	}
	
	private static void trainingSetGeneration(Path sourceDir){
		Path targetDir = Paths.get("training_data/");
		nb.generateTrainingSet(sourceDir, targetDir, excludefile);
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
		// set defaults
		if(!ops.containsKey("seed")){
			ops.put("seed", new ArrayList<String>());
			ops.get("seed").add("0");
		}
		if(!ops.containsKey("lr")){
			ops.put("lr", new ArrayList<String>());
			ops.get("lr").add("0.006");
		}
		if(!ops.containsKey("epochs")){
			ops.put("epochs", new ArrayList<String>());
			ops.get("epochs").add("15");
		}
		int[] hidden;
		if(ops.containsKey("hidden")){
			hidden = ops.get("hidden").stream().mapToInt(Integer::parseInt).toArray();
		} else {
			hidden = new int[]{512,256,128,128};
		}
		
		for(String lrStr : ops.get("lr")){
			double lr = Double.parseDouble(lrStr);
			
			for(String epochsStr : ops.get("epochs")){
				int epochs = Integer.parseInt(epochsStr);
				
				for(String seedStr : ops.get("seed")){
					int seed = Integer.parseInt(seedStr);
					System.out.println(lr);
					buildNet(seed, lr, hidden);
					
					try {
						nb.train(traincsv, testcsv, epochs);
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
	}

}
