import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Stream;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.storage.FileStatsStorage;

import neurob.core.NeuroB;
import neurob.core.features.PredicateImages;
import neurob.core.features.TheoryFeatures;
import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.core.features.interfaces.FeatureGenerator;
import neurob.core.nets.NeuroBConvNet;
import neurob.core.nets.NeuroBNet;
import neurob.core.util.SolverType;
import neurob.exceptions.NeuroBException;
import neurob.training.DataDumpTranslator;
import neurob.training.TrainingSetAnalyser;
import neurob.training.TrainingSetGenerator;
import neurob.training.generators.PredicateDumpGenerator;
import neurob.training.generators.TrainingDataGenerator;
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

					+ "trainingset -split -source <source> -first <first> -second <second> -ratio <ratio> [-net <features> <labels>]\n"
					+ "\tSplit the training set located in <source> into two distinct subsets, <first> and <second>\n"
					+ "\t<first> will hold <ratio> times of samples from <source>, <second> will hold 1-<ratio>\n"
					+ "\twith <ratio> being a number from the interval [0,1]\n"

					+ "trainingset -trim -source <source> -target <target> [-net <features> <labels>]\n"
					+ "\tTrims the training set located in <source> after a beforehand analysis\n"
					+ "\tThe trimmed version of the training set will be located in <target> afterwards.\n"

					+ "pdump -dir <directory> [-excludefile <excludefile>]\n"
					+ "\tCreates predicate dump files from the (Event)B machines in <directory>\n"

					+ "pdump -file <file>\n"
					+ "\tCreates predicate dump files from <file>\n"

					+ "pdump -translate <directory> [-net <features> <labels>]\n"
					+ "\tTranslates the given directory of pdump-files into a CSV to train the given neural network\n"

					+ "pdump -analyse <directory>\n"
					+ "\tAnalyses the .pdump files in the given directory (recursively)\n"

					+ "pdump -split <source> -first <first> -second <second> -ratio <ratio>\n"
					+ "\tSplit the training set located in <source> into two distinct subsets, <first> and <second>\n"
					+ "\t<first> will hold <ratio> times of samples from <source>, <second> will hold 1-<ratio>\n"
					+ "\twith <ratio> being a number from the interval [0,1]\n"

					+ "pdump -trim <directory> -target <directory> -solver <solver>\n"
					+ "\tTrims the predicate dump given with respect to the given solver as classification problem\n"
					+ "\tThe trimmed set will be located in the target directory; the source set remains unchanged\n"
					+ "\t<solver> may be one of the following:\n"
					+ "\t\tprob, kodkod, z3, smt\n"
					+ "\t\twith smt representing SMT_SUPPORTED_INTERPRETER, a combination of ProB and Z3\n"

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

					+ "loadnet -dl4jdata <modeldirectory> [-net <features> <labels>]\n"
					+ "\tLoad stats of an already trained model into the DL4J UI"

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
					+ "\t\tprob (default), kodkod, z3,\n"
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
			else if(ops.containsKey("split")){
				if(ops.containsKey("source")){
					if(ops.containsKey("first") && ops.containsKey("second") && ops.containsKey("ratio")){
						Path csv = Paths.get(ops.get("source").get(0));
						Path first = Paths.get(ops.get("first").get(0));
						Path second = Paths.get(ops.get("second").get(0));
						double ratio = Double.parseDouble(ops.get("ratio").get(0));
						splitTrainingset(csv, first, second, ratio);

					}
					else {
						System.out.println("trainingset -split: Missing at least one of those parameters: -first, -second, -ratio");
					}
				}
				else {
					System.out.println("trainingset -split: Missing -source parameter");
				}
			}
			else if(ops.containsKey("trim")){
				if(ops.containsKey("source") && ops.containsKey("target")){
					Path source = Paths.get(ops.get("source").get(0));
					Path target = Paths.get(ops.get("target").get(0));
					trimTrainingSet(source, target);
				} else {
					System.out.println("trainingset -trim: -source and -target arguments need to be set");
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
			else if(ops.containsKey("file")){
				Path file = Paths.get(ops.get("file").get(0));
				generatePDumpFromFile(file);
			}
			else if(ops.containsKey("translate")){
				buildNet();
				Path dir = Paths.get(ops.get("translate").get(0));
				translatePDump(dir);
			}
			else if(ops.containsKey("analyse")){
				Path dir = Paths.get(ops.get("analyse").get(0));
				analysePDump(dir);
			}
			else if(ops.containsKey("trim")){
				Path dir = Paths.get(ops.get("trim").get(0));
				Path target = Paths.get(ops.get("target").get(0));
				trimPDump(dir, target, ops.get("solver").get(0));
			}
			else if(ops.containsKey("split")){
				if(ops.containsKey("first") && ops.containsKey("second") && ops.containsKey("ratio")){
					Path csv = Paths.get(ops.get("split").get(0));
					Path first = Paths.get(ops.get("first").get(0));
					Path second = Paths.get(ops.get("second").get(0));
					double ratio = Double.parseDouble(ops.get("ratio").get(0));
					splitPDump(csv, first, second, ratio);

				}
				else {
					System.out.println("pdump -split: Missing at least one of those parameters: -first, -second, -ratio");
				}
			}
			else {
				System.out.println("pdump: expecting either -file, -dir, or -translate parameter");
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
		else if(cmd.equals("loadnet")){
			if(ops.containsKey("dl4jdata")){
				Path dl4jData = Paths.get(ops.get("dl4jdata").get(0));
				loadDL4JData(dl4jData);
			}
			else {
				System.out.println("loadnet: missing -dl4jdata parameter");
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

	private static void splitPDump(Path dir, Path first, Path second, double ratio){
		PredicateDumpGenerator gen = new PredicateDumpGenerator();

		try {
			gen.splitTrainingData(dir, first, second, ratio, new Random(123));
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}

	private static void trimPDump(Path dir, Path target, String solver) {
		// get solver type
		SolverType trimSolver = null;
		if(solver.equals("prob")){
			trimSolver = SolverType.PROB;
		} else if(solver.equals("kodkod")){
			trimSolver = SolverType.KODKOD;
		} else if(solver.equals("z3")){
			trimSolver = SolverType.Z3;
		} else if(solver.equals("smt")){
			trimSolver = SolverType.SMT_SUPPORTED_INTERPRETER;
		}

		PredicateDumpGenerator gen = new PredicateDumpGenerator(3, trimSolver);

		try {
			gen.trimTrainingData(dir, target);
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}

	private static void loadDL4JData(Path dl4jData) {
		StatsStorage stats = new FileStatsStorage(dl4jData.toFile());
		UIServer ui = UIServer.getInstance();
		ui.attach(stats);
		System.out.println("DL4J UI is available at http://localhost:9000/ for 10 seconds");
		System.out.println("To stop the server, kill this process (CTRL+C)");
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void trimTrainingSet(Path source, Path target) {
		FeatureGenerator fg = getFeatureGenerator();
		LabelGenerator lg = getLabelGenerator();

		TrainingDataGenerator tdg = fg.getTrainingDataGenerator(lg);

		try {
			tdg.trimTrainingData(source, target);
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}

	private static void splitTrainingset(Path source, Path first, Path second, double ratio) {
		FeatureGenerator fg = getFeatureGenerator();
		LabelGenerator lg = getLabelGenerator();

		TrainingDataGenerator tdg = fg.getTrainingDataGenerator(lg);

		try {
			tdg.splitTrainingData(source, first, second, ratio, new Random(123));
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}

	private static void generatePDumpFromFile(Path file) {
		PredicateDumpGenerator tpg = new PredicateDumpGenerator();

		try {
			Path targetFile = tpg.generateTrainingDataPath(file, Paths.get("training_data/PredicateDump/"));
			tpg.generateTrainingDataFromFile(file, targetFile);
		} catch (NeuroBException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void generatePDump(Path dir) {
		TrainingSetGenerator tpd = new TrainingSetGenerator(new PredicateDumpGenerator());
		tpd.generateTrainingSet(dir, Paths.get("training_data/PredicateDump/"), excludefile);
	}

	private static void translatePDump(Path dir) {
//		try {
			Path target = Paths.get("training_data/"+nb.getNeuroBNet().getDataPathName());
			nb.getNeuroBNet().getTrainingSetGenerator().translateDataDumpFiles(dir, target);
//		} catch (NeuroBException e) {
//			e.printStackTrace();
//		}
	}

	private static void analysePDump(Path dir){
		try {
			TrainingSetAnalyser.logTrainingAnalysis(TrainingSetAnalyser.analysePredicateDumps(dir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void buildNet(){
		buildNet(1);
	}

	private static void buildNet(int i){
		buildNet(i, 0.006, new int[]{512, 256, 128, 128});
	}

	private static void buildNet(int seed, double learningrate, int[] hiddenLayers){
		NeuroBNet model;
		LabelGenerator labelling = getLabelGenerator();
		FeatureGenerator features = getFeatureGenerator();

		// FeatureGenerator features;
		if(features instanceof ConvolutionFeatures){
			model = new NeuroBConvNet(hiddenLayers, learningrate, (ConvolutionFeatures) features, labelling, seed);
		}
		else {//if(feats.equals("predf")){
			model = new NeuroBNet(hiddenLayers, learningrate, features, labelling, seed);
		}

		nb = new NeuroB(model);
		nb.enableDL4JUI(true);
	}

	private static FeatureGenerator getFeatureGenerator(){
		// get net type
		String feats = "predf";
		if(ops.containsKey("net")){
			ArrayList<String> net = ops.get("net");
			if(net.size() != 2){
				System.out.println("-net expects two parameters: <features> and <labels>");
				System.exit(10);
			}
			feats = net.get(0);
		}

		if(feats.equals("predi")){
			int s = 32;
			if(ops.containsKey("size")){
				s = Integer.parseInt(ops.get("size").get(0));
			}
			return new PredicateImages(s);
		}
		else {
			return new TheoryFeatures();
		}
	}

	private static LabelGenerator getLabelGenerator(){
		String label = "solclass";
		if(ops.containsKey("net")){
			ArrayList<String> net = ops.get("net");
			if(net.size() != 2){
				System.out.println("-net expects two parameters: <features> and <labels>");
				System.exit(10);
			}
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
				else if(sstring.equals("z3"))
					solver = SolverType.Z3;
				else if(sstring.equals("smt"))
					solver = SolverType.SMT_SUPPORTED_INTERPRETER;
			}
			labelling = new SolverClassificationGenerator(solver);
		}

		return labelling;
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
		try {
			nb.generateTrainingSet(sourceDir, targetDir, excludefile);
		} catch (NeuroBException e) {
			e.printStackTrace();
		}
	}

	private static void analyseTrainingSet(Path dir, TrainingSetGenerator tsg) {
		try {
			TrainingSetAnalyser.logTrainingAnalysis(tsg.analyseTrainingSet(dir));
		} catch (NeuroBException e) {
			System.out.println("Could not access target directory "+dir);
			e.printStackTrace();
		}
	}

	private static void analyseTrainingSetCSV(Path csv, TrainingSetGenerator tsg) {
		try {
			TrainingSetAnalyser.logTrainingAnalysis(TrainingSetAnalyser.analyseTrainingCSV(csv, getLabelGenerator()));
		} catch (IOException e) {
			System.out.println("Could not access target file "+csv);
			e.printStackTrace();
		}
	}

	private static void exclude(Path excludefile, Path excl) {
		TrainingSetGenerator tsg = new TrainingSetGenerator(
				new TheoryFeatures().getTrainingDataGenerator(new SolverClassificationGenerator(SolverType.PROB)));
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
						nb.train(traincsv, testcsv, epochs, true);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
