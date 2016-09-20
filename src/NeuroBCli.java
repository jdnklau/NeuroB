import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import neurob.NeuroB;
import neurob.core.nets.DefaultPredicateSolverPredictionNet;
import neurob.core.nets.interfaces.NeuroBNet;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.DefaultTrainingDataCollector;

public class NeuroBCli {
	private static final Path libraryIOpath = Paths.get("prob_examples/LibraryIO.def");
	private static NeuroB nb;
	private static Path dir;
	private static Path tar;
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
			cmd = "trainingset"; // default
		}
		else {
			cmd = args[0];
		}
		
		// execute command
		
		if(cmd.equals("help")){
			String help =
					  "Call with the following arguments, where -net <net> indicates the neural net to be used (see below):\n"
					
					+ "trainingset [-dir <directory>] [-net <net>] [-excludefile <excludefile]\n"
					+ "\tGenerate training data from the mch files found in <directory>, but ignore those listed in <excludefile>\n"
					
					+ "trainingset [-dir <directory>] -file <filename> [-net <net>]\n"
					+ "\tGenerate training data from a specific file. <filename> has to be given relative to <directory>, which contains <file>\n"
					
					+ "trainingset -analyse [-tar <directory>] [-net <net>]\n"
					+ "\tAnalyse the generated training data in <directory>\n"

					+ "trainingset -csv [-tar <directory>]\n"
					+ "\tGenerate csv file from nbtrain files in <directory>\n"
					
					+ "libraryIODef -dir <directory>\n"
					+ "\tDistributes the LibraryIO.def file in <directory>\n"

					+ "exclude [-excludefile <excludefile>] -source <toexcludes>\n"
					+ "\tSets <toexclude> (either path to directory or file) onto the specified <excludefile>, if not already present\n"
					+ "\t<toexcludes> can be a list of multiple paths to files or directories, separated by a blank space\n"
					
					+ "\nDefault values:\n"
					+ "- if -dir <directory> is not set, it defaults to prob_examples/public_examples/B/\n"
					+ "- if -tar <directory> is not set, it defaults to training_data/manual_call/\n"
					+ "- if -net <net> is not set, it defaults to 'default' net\n"
					+ "- if -excludefile <excludefile> is not set, it defaults to prob_examples/default.excludes"
					
					+ "\nNets:\n"
					+ "The implemented nets you can access via the cli are\n"
					+ "\tdefault - The default implementation"
					;
			
			System.out.println(help);
							  
		}
		// Generate training data
		else if(cmd.equals("trainingset")){
			// analyse training set
			if(ops.containsKey("analyse")){
				analyseTrainingSet(tar);
			}
			// generate csv
			else if(ops.containsKey("csv")){
				trainingCSVGeneration(tar);
			}
			// generate single nbtrain file
			else if(ops.containsKey("file")){
				Path sourcefile = dir.resolve(ops.get("file").get(0));
				singleTrainingDataGeneration(sourcefile);
			}
			// generate training set
			else {
				trainingSetGeneration(dir);
			}			
		}
		// distribute library file
		else if(cmd.equals("libraryIODef")){
			distribute(dir);
		}
		// handle excludes
		else if(cmd.equals("exclude")){
			for(String s : ops.get("source")){
				exclude(excludefile, Paths.get(s));
			}
		}
	}

	private static void parseCommandLineOptions(HashMap<String, ArrayList<String>> ops) {
		// the net to use
		String net = "default";
		if(ops.containsKey("net")){
			net = ops.get("net").get(0);
		}
		// setting up the net
		NeuroBNet nbn = new DefaultPredicateSolverPredictionNet();
		// future nets to come here
		nb = new NeuroB(nbn.setSeed(0L).build());
		
		// load directory
		if(ops.containsKey("dir")){
			dir = Paths.get(ops.get("dir").get(0));
		}
		else {
			// default
			dir = Paths.get("prob_examples/public_examples/B/");
		}
		
		if(ops.containsKey("tar")){
			tar = Paths.get(ops.get("tar").get(0));
		}
		else {
			// default
			tar = Paths.get("training_data/manual_call/");
		}
		
		// exclude file
		if(ops.containsKey("excludefile")){
			excludefile = Paths.get(ops.get("excludefile").get(0));
		}
		else {
			// default
			excludefile = Paths.get("prob_examples/default.excludes");
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
	            		System.out.println("NOT created (maybe already existing) "+newLibraryIOPath+": "+e.getMessage());
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
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		Path targetDir = Paths.get("training_data/manual_call/public_examples/B/");
		
		String fileName = source.getFileName().toString();
		Path target = source.resolve(fileName.substring(0, fileName.lastIndexOf('.'))+".nbtrain");
		tsg.generateTrainingDataFile(source, target);
	}
	
	private static void trainingSetGeneration(Path sourceDir){
		Path targetDir = Paths.get("training_data/");
		
		nb.generateTrainingSet(sourceDir, targetDir, excludefile);
	}
	
	private static void analyseTrainingSet(Path dir){
		TrainingSetAnalyser tsa = new TrainingSetAnalyser();
		tsa.analyseTrainingSet(dir);
		System.out.println(tsa.getStatistics());
	}
	
	private static void trainingCSVGeneration(Path dir){
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		
		Path target = Paths.get("training_data/manual_call/data.csv");
		
		tsg.generateCSVFromNBTrainData(dir, target);
	}
	
	private static void exclude(Path excludefile, Path excl) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(new DefaultTrainingDataCollector());
		tsg.exclude(excludefile, excl);
		
	}

}
