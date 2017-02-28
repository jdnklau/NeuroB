import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.codehaus.groovy.transform.trait.TraitASTTransformation;

import de.prob.scripting.ModelTranslationError;
import neurob.core.NeuroB;
import neurob.core.features.CodeImages;
import neurob.core.features.PredicateFeatures;
import neurob.core.nets.NeuroBConvNet;
import neurob.core.nets.NeuroBNet;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.TrainingPredicateDumper;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.interfaces.LabelGenerator;
import neurob.training.generators.labelling.SolverClassificationGenerator;
import neurob.training.generators.labelling.SolverSelectionGenerator;
import neurob.training.generators.labelling.SolverTimerGenerator;

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
					  "Call with the following arguments, where -net <features> <labels> indicates the neural net to be used (see below):\n"
					
					+ "trainingset -dir <directory> [-net <features> <labels>] [-excludefile <excludefile]\n"
					+ "\tGenerate training data from the mch files found in <directory>, but ignore those listed in <excludefile>\n"
					
					+ "trainingset -file <filename +> [-net <features> <labels>]\n"
					+ "\tGenerate training data from the specified files, separated by a space character \n"
					
					+ "trainingset -analyse -dir <directory> [-net <features> <labels>]\n"
					+ "\tAnalyse the generated training data in <directory>\n"
					+ "\t<net> specifies the label format in use; specifically whether it is regression data or not\n"
					
					+ "trainingset -analyse -file <file> [-net <features> <labels>]\n"
					+ "\tAnalyse the generated training data in <file>\n"
					+ "\t<net> specifies the label format in use; specifically whether it is regression data or not\n"

					+ "trainingset -csvsplit -file <file> -first <first> -second <second> -ratio <ratio>\n"
					+ "\tSplit a given CSV file into <first> and <second>, both being CSV files again\n"
					+ "\tFor this, the given <ratio> is used, a number in the interval [0,1]\n"
					
					+ "trainingset -csvtranslate -file <csv> -dir <directory> [-net <features> <labels>]\n"
					+ "\tFor models with convolutional features, this translates the data.csv from training set generation into\n"
					+ "\ta directory structure in <directory>, that contains the classes as subdirectories and places all\n"
					+ "\tsamples as image files accordingly inside one of them\n"
					
					+ "trainingset -csvgenerate -dir <directory> [-net <features> <labels>]\n"
					+ "\tCreates a data.csv from found .nbtrain files in the given <directory>\n"
					
					+ "pdump -dir <directory> [-excludefile <excludefile]\n"
					+ "\tCreates predicate dump files from the (Event)B machines in <directory>\n"
					
					+ "pdump -file <file>\n"
					+ "\tCreates predicate dump files from <file>\n"
					
					+ "trainnet -train <trainingfile> -test <testfile> [-net <features> <labels>]\n"
					+ "\tTrains a neural net with the given <trainingfile> and evaluates the training step on the given <testfile>\n"
					+ "\tBoth files being csv files generated with this tool\n"
					
					+ "trainnet -train <traindata> -test <testdata> [-hidden <layer_sizes +>] [-seed <seed +>] [-epochs <epochs +>] [-lr <learningrate +>] [-net <features> <labels>]\n"
					+ "\tTrains a neural networks model of type <net>\n"
					+ "\tThe model is trained on <traindata>, then evaluated on <testdata>\n"
					+ "\t-hidden determines the number and size of hidden layers; -hidden 256 128 128 would create 3 hidden layers with respective amount of neurons\n"
					+ "\t\tDefault: -hidden 512 256 128 128\n"
					+ "\tThe defaults for the other hyper parameters are seed: 0, epochs: 15, learningrate: 0.006\n"
					+ "\t\tNote: One can set multiple values for the hyper parameters seed, epochs, and lr, resulting in training each possible combination\n"
					+ "\t\t      so be carefull with how many you query\n"
					+ "\t\tExample: -seed 1 2 -lr 0.006 0.0007\n"
					
					+ "libraryIODef -dir <directory>\n"
					+ "\tDistributes the LibraryIO.def file in <directory>\n"

					+ "exclude [-excludefile <excludefile>] -source <toexcludes>\n"
					+ "\tSets <toexclude> (path to either directory or file) onto the specified <excludefile>, if not already present\n"
					+ "\t<toexcludes> can be a list of multiple paths to files or directories, separated by a blank space\n"
					
					+ "\nDefault values:\n"
					+ "- if -excludefile <excludefile> is not set, it defaults to default.excludes\n"
					+ "\t* if -excludefile none is set, no exclusions are made\n"
					
					+ "\nNets:\n"
					+ "The -net argument takes two parameters: <features> and <labels>.\n"
					
					+ "<features> can be one of the following:\n"
					+ "\tpredf: (default) Basic, handcrafted features for predicates\n"
					+ "\tpredi: Predicate image features, i.e. image versions of the predicates\n"
					+ "\t\tTakes optional -size <s> parameter, generating <s>**2 sized images (default: 32)\n"
					
					+ "<labels> describe the labelling mechanism in use:\n"
					+ "\tsolclass: (default) Solver classification; classifies whether a given solver can decide a predicate or not\n"
					+ "\t\tTakes optional -solver <solver> argument, with <solver> being\n"
					+ "\t\tprob (default), kodkod,\n"
					+ "\t\tor smt (for SMT_SUPPORTED_INTERPRETER setting in ProB, using ProB+Z3 together)\n"
					+ "\tsolsel: Selection approach, what solver decides a given predicate the fastes\n"
					+ "\tsoltime: Regression approach for each solver, how long it takes to decide a predicate\n"
					
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
			else if(ops.containsKey("csvgenerate")){
				if(ops.containsKey("dir")){
					buildNet();
					csvgenerate(dir);
				} 
				else {
					System.out.println("trainingset -csvgenerate: Missing -dir parameter");
				}
			} 
			else if(ops.containsKey("dir")){// generate training set
				buildNet();
				trainingSetGeneration(dir);
			}
			// generate single nbtrain file
			else if(ops.containsKey("file")){
					buildNet();
					Path sourcefile;
					for(String file : ops.get("file")){
						sourcefile= Paths.get(file);
						singleTrainingDataGeneration(sourcefile);
					}
				}
			// nope
			else {
				System.out.println("trainingset: missing at least -dir parameter");
			}
		}
		// pdump
		else if(cmd.equals("pdump")){
			if(ops.containsKey("dir")){
				Path dir = Paths.get(ops.get("dir").get(0));
				generatePDump(dir);
			}
			else if(ops.containsKey("dir")){
				Path dir = Paths.get(ops.get("dir").get(0));
				generatePDumpFromFile(dir);
			}
			else {
				System.out.println("pdump: expecting either -file or -dir parameter");
			}
		}
		// trainnet -train <traindata> -test <testdata> [-seed <seed>+] [-epochs <epochs>+] [-lr <learningrate>+] [-net <features> <labels>]
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
	
	private static void generatePDumpFromFile(Path dir) {
		TrainingPredicateDumper tpd = new TrainingPredicateDumper();
		try {
			tpd.createPredicateDumpFromFile(dir, Paths.get("training_data/PredicateDump/"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ModelTranslationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void generatePDump(Path dir) {
		TrainingPredicateDumper tpd = new TrainingPredicateDumper();
		tpd.createPredicateDump(dir, Paths.get("training_data/PredicateDump/"), excludefile);
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
		String feats = "predf";
		String label = "solclass";
		if(ops.containsKey("net")){
			ArrayList<String> net = ops.get("net");
			if(net.size() != 2){
				System.out.println("-net expects two parameters: <features> and <labels>");
				System.exit(10);
			}
			feats = net.get(0);
			label = net.get(1);
		}
		
		LabelGenerator labelling;
		if(label.equals("soltime")) {
			labelling = new SolverTimerGenerator();
		} else if(label.equals("solsel")){
			labelling = new SolverSelectionGenerator();
		} else { // if (label.equals("solclass")) {
			SolverType solver = SolverType.PROB;
			if(ops.containsKey("solver")){
				String sstring = ops.get("solver").get(0);
				if(sstring.equals("kodkod"))
					solver = SolverType.KODKOD;
				else if(sstring.equals("smt"))
					solver = SolverType.SMT_SUPPORTED_INTERPRETER;
			}
			labelling = new SolverClassificationGenerator(solver);
		}
		
		// FeatureGenerator features;
		if(feats.equals("predi")){
			int s = 32;
			if(ops.containsKey("size")){
				s = Integer.parseInt(ops.get("size").get(0));
			}
			model = new NeuroBConvNet(hiddenLayers, learningrate, new CodeImages(s), labelling, seed);
		}
		else {//if(feats.equals("predf")){
			model = new NeuroBNet(hiddenLayers, learningrate, new PredicateFeatures(), labelling, seed);
		}
		
		nb = new NeuroB(model);
		nb.enableDL4JUI(true);
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
		buildNet();
		
		String fileName = source.getFileName().toString();
		Path target = Paths.get("training_data/single_file_generation/").resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
		nb.getNeuroBNet().getTrainingSetGenerator().generateTrainingDataFromFile(source, target);
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
	
	private static void csvgenerate(Path dir){
		Path csv = dir.resolve("data.csv");
		nb.getNeuroBNet().getTrainingSetGenerator().generateCSVFromNBTrainFiles(dir, csv);
	}
	
	private static void exclude(Path excludefile, Path excl) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(new PredicateFeatures(), new SolverClassificationGenerator(SolverType.PROB));
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
