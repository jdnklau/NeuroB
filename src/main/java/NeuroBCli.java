import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import neurob.core.NeuroB;
import neurob.core.nets.KodKodPredictionNet;
import neurob.core.nets.PredicateSolverPredictionNet;
import neurob.core.nets.PredicateSolverSelectionNet;
import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.SolverClassificationDataCollector;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");
	private static NeuroB[] nbs;
	private static Path dir;
	private static HashMap<String, ArrayList<String>> ops;
	private static Path excludefile;

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
					
					+ "trainingset -analyse -dir <directory> [--log-relevant-files]\n"
					+ "\tAnalyse the generated training data in <directory>\n"
					+ "\tIf --log-relevant-files is used, files of interest have their names logged into a special log file\n"

					+ "trainingset -csv -dir <directory> [--ignoreEquallyLabeledEntries]\n"
					+ "\tGenerate csv file from nbtrain files in <directory>\n"
					+ "\tIf --ignoreEquallyLabeledEntries is set, all data vectors with multi classification, that map to each class are ignored\n"
					
					+ "trainnet -file <file> [-net <net>]\n"
					+ "\tTrains a neural net with the given <file> (being a csv generated with this tool)\n"
					
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
					+ "\tpsp - Predicate Solver Prediction\n"
					+ "\tpss - Predicate Solver Selection\n"
					+ "\tkodkod - KodKod only prediction"
					;
			
			System.out.println(help);
							  
		}
		// Generate training data
		else if(cmd.equals("trainingset")){
			// generate single nbtrain file
			if(ops.containsKey("file")){
				buildNet();
				Path sourcefile = Paths.get(ops.get("file").get(0));
				singleTrainingDataGeneration(sourcefile);
			}
			else if(ops.containsKey("dir")){
				// analyse training set
				if(ops.containsKey("analyse")){
					analyseTrainingSet(dir, ops.containsKey("-log-relevant-files"));
				}
				// generate csv
				else if(ops.containsKey("csv")){
					trainingCSVGeneration(dir, ops.containsKey("-ignoreEquallyLabeledEntries"));
				}
				else {
				// generate training set
					buildNet();
					trainingSetGeneration(dir);
				}
			}
			// nope
			else {
				System.out.println("trainingset: missing at least -dir parameter");
			}
		}
		else if(cmd.equals("trainnet")){
			if(ops.containsKey("file")){
				if(ops.containsKey("n")){
					int num = Integer.parseInt(ops.get("n").get(0));
					buildNets(num);
				} else {
					buildNet();
				}
				Path sourcefile = Paths.get(ops.get("file").get(0));
				trainNet(sourcefile);
			}
			// nope
			else {
				System.out.println("trainnet: missing -file parameter");
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
	}
	
	private static void buildNet(){
		buildNets(1);
	}
	
	private static void buildNets(int num){
		// Net to use
		NeuroBNet[] nets = new NeuroBNet[num];
		nbs = new NeuroB[num];
		// get net type
		String net = "psp";
		if(ops.containsKey("net")){
			net = ops.get("net").get(0);
		}
		// set up nets
		for(int i=0; i<num; i++){
			if(net.equals("kodkod")){
				nets[i] = new KodKodPredictionNet();
			} else if(net.equals("pss")){
				nets[i] = new PredicateSolverSelectionNet();
			} else if(net.equals("psp")){
				nets[i] = new PredicateSolverPredictionNet();
			}else {
				nets[i] = new PredicateSolverPredictionNet();
				System.out.println("Net "+net+" is not known; defaulting to psp.");
			}
			
			nbs[i] = new NeuroB(nets[i].setSeed((long)i).build());
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
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
			
	        for (Path entry : stream) {

	        	// check if directory or not; recursion if so
	            if (Files.isDirectory(entry)) {
	            	
	            	Path newLibraryIOPath = entry.resolve("LibraryIO.def");
	            	try{
	            		Files.copy(libraryIOpath, newLibraryIOPath);
	            		System.out.println("Created: "+newLibraryIOPath);
	            	} catch (IOException e){
//	            		System.out.println("Already present: "+newLibraryIOPath+": "+e.getMessage());
	            		/*
	            		 * NOTE:
	            		 * This catch does nothing, to prevent the permanent printing of the message you can see
	            		 * there which is commented out.
	            		 * The makefile tries for now to ensure this method is called before starting to generate a training set.
	            		 * This is a little bit annoying, hence the catch block doing nothing.
	            		 */
	            	}
	            	
	            	distribute(entry); //  distribute the file recusively
	            	
	            }
	            
	        }
	    }
		catch (IOException e){
			System.out.println("Could not access directory "+directory+": "+e.getMessage());
		}
	}
	
	private static void singleTrainingDataGeneration(Path source){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new SolverClassificationDataCollector());
		
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
	
	private static void analyseTrainingSet(Path dir, boolean logFiles){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(dir, logFiles);
		tsa.logStatistics();
	}
	
	private static void trainingCSVGeneration(Path dir, boolean ignore){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new SolverClassificationDataCollector());
		Path target = Paths.get("training_data/manual_call/data.csv");
		
		tsg.generateCSVFromNBTrainData(dir, target, ignore);
	}
	
	private static void exclude(Path excludefile, Path excl) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(new SolverClassificationDataCollector());
		tsg.exclude(excludefile, excl);
		
	}
	
	private static void trainNet(Path csv){
		for(NeuroB nb : nbs){
			try {
				nb.train(csv);
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
