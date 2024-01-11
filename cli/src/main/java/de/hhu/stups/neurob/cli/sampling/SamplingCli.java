package de.hhu.stups.neurob.cli.sampling;

import de.hhu.stups.neurob.cli.BackendId;
import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.SimplePredicateList;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import de.hhu.stups.neurob.training.generation.util.PredicateCollection;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SamplingCli implements CliModule {

    private Options options;

    public SamplingCli() {
        options = new Options();
        initOptions();
    }

    @Override
    public String getUsageInfo() {
        return "sampling -f MACHINE_FILE -a ALPHA -e ERROR -s SAMPLING_SIZE [-[x]b BACKENDS]\n"
        + "       sampling -p PRED_LIST_FILE MCH_DIR -a ALPHA -e ERROR -s SAMPLING_SIZE [-[x]b BACKENDS]\n";
    }

    private void initOptions() {
        Option mchFile = Option.builder("f")
                .longOpt("file")
                .hasArg()
                .argName("MACHINE_FILE")
                .desc("Calculate confidence interval over generated predicates from the given machine file.")
                .build();

        Option lstFile = Option.builder("p")
                .longOpt("predlist")
                .hasArgs()
                .numberOfArgs(2)
                .argName("PRED_LIST_FILE")
                .desc("Calculate confidence interval over the first predicates in the given predicate file.")
                .build();

        Option lstFile2 = Option.builder("l")
                .longOpt("predlist")
                .hasArgs()
                .numberOfArgs(2)
                .argName("PRED_LIST_FILE")
                .desc("Calculate confidence interval over the first predicates in the given predicate file.")
                .build();

        Option alpha = Option.builder("a")
                .longOpt("alpha")
                .hasArg()
                .argName("ALPHA")
                .desc("Alpha-value for confidence level: 0.1 correlates to 90 % confidence.")
                .required()
                .build();

        Option error = Option.builder("e")
                .longOpt("")
                .hasArg()
                .argName("ERROR")
                .desc("Allowed error; results in an allowed error of +/- ERROR ms.")
                .required()
                .build();

        Option samplingSize = Option.builder("s")
                .longOpt("sampling-size")
                .hasArg()
                .argName("SIZE")
                .desc("Maximum number of samplings to be done.")
                .required()
                .build();

        Option backends = Option.builder("b")
                .longOpt("backends")
                .hasArgs()
                .desc("BackendId to be accounted for.")
                .build();
        Option cross = Option.builder("x")
                .longOpt("cross-options")
                .desc("If set, enumerates any combination of options per backend.")
                .build();

        Option probHome = Option.builder("h")
                .longOpt("prob-home")
                .hasArg()
                .argName("PATH")
                .desc("If set, uses the ProB cli located at the given path.")
                .build();

        Option maxPreds = Option.builder("n")
                .longOpt("max-preds")
                .hasArg()
                .argName("NUM")
                .desc("Sets the maximum number of predicates to process. Defaults to 20.")
                .build();

        options.addOption(mchFile);
        options.addOption(lstFile);
        options.addOption(lstFile2);
        options.addOption(alpha);
        options.addOption(error);
        options.addOption(samplingSize);
        options.addOption(backends);
        options.addOption(cross);
        options.addOption(probHome);
        options.addOption(maxPreds);
    }

    @Override
    public String getHelpText() {
        return null;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public void eval(String[] args) throws Exception {
        int maxPreds = 20;

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("n")) {
                maxPreds = Integer.parseInt(line.getOptionValue("n"));
            }

            // Check for ProB home
            if (line.hasOption("h")) {
                setProBHomeFromOption(line, "h");
            }

            List<MchPredPair> preds;
            if (line.hasOption('f')) {
                Path listFile = Paths.get(line.getOptionValue('f'));

                MachineAccess mch = new MachineAccess(listFile);
                PredicateCollection pc = new PredicateCollection(mch);

                List<BPredicate> bpreds = new ArrayList<>();

                bpreds.addAll(FormulaGenerator.assertions(pc));
                bpreds.addAll(FormulaGenerator.enablingAnalysis(pc));
                bpreds.addAll(FormulaGenerator.extendedPreconditionFormulae(pc));
                bpreds.addAll(FormulaGenerator.invariantConstrains(pc));
                bpreds.addAll(FormulaGenerator.invariantPreservationFormulae(pc));
                bpreds.addAll(FormulaGenerator.multiPreconditionFormulae(pc));
                bpreds.addAll(FormulaGenerator.weakestPreconditionFormulae(pc));

                if (bpreds.size() > maxPreds) {
                    Collections.shuffle(bpreds, new Random(20231124L));
                    System.out.println("Using random selection of " + maxPreds + " predicates.");
                }

                preds = bpreds.stream()
                        .map(p -> new MchPredPair(listFile, p))
                        .limit(maxPreds)
                        .collect(Collectors.toList());
            } else if (line.hasOption('p')) {
                Path file = Paths.get(line.getOptionValues('p')[0]);
                Path dir = Paths.get(line.getOptionValues('p')[1]);

                SimplePredicateList pl = new SimplePredicateList();
                preds = pl.loadSamples(file)
                        .map(TrainingSample::getLabelling)
                        .map(entry -> new MchPredPair(
                                dir.resolve(entry.getSource().getLocation()),
                                entry.getPredicate()))
                        .limit(maxPreds)
                        .collect(Collectors.toList());
            } else if (line.hasOption('l')) {
                Path file = Paths.get(line.getOptionValues('l')[0]);
                Path dir = Paths.get(line.getOptionValues('l')[1]);

                preds = Files.lines(file)
                        .map(s -> {
                            int idx = s.indexOf(':');
                            Path f = dir.resolve(Paths.get(s.substring(0,idx)));
                            BPredicate b = BPredicate.of(s.substring(idx+1));
                            return new MchPredPair(f,b);
                        })
                        .limit(maxPreds)
                        .collect(Collectors.toList());
            } else {
                System.out.println("Missing either -f or -p option.");
                System.exit(1);
                return;
            }

            double alpha = Double.parseDouble(line.getOptionValue('a'));
            double error = Double.parseDouble(line.getOptionValue('e'));
            int sampSize = Integer.parseInt(line.getOptionValue('s'));
            List<Backend> backends = parseBackends(line);

            calculateConfidence(preds, alpha, error, sampSize, backends);

        } catch (ParseException e) {
            System.out.println("Unable to get command line arguments: " + e);
        }
    }

    static void setProBHomeFromOption(CommandLine line, String fromOption) {
        String probHome = line.getOptionValue(fromOption);
        System.setProperty("prob.home", probHome);
    }

    void calculateConfidence(
            List<MchPredPair> pairs,
            double alpha, double error, int sampSize,
            List<Backend> backends) {

        List<BPredicate> preds = pairs.stream().map(MchPredPair::getPred).collect(Collectors.toList());
        // Generate samples first.
        System.out.println("Gathered " + preds.size() + " predicates.");

        // Gather runtimes
        Map<BPredicate, List<PredDbEntry>> dbEntries = new HashMap<>();
        PredDbEntry.Generator gen =
                new PredDbEntry.Generator(1, backends.toArray(new Backend[0]));
        pairs.forEach(line -> {
            try {
                BPredicate p = line.getPred();
                MachineAccess mch = new MachineAccess(line.getMch(), MachineType.CLASSICALB, false);

                dbEntries.put(p, new ArrayList<>());
                for (int i = 0; i < sampSize; i++) {
                    mch.load(); // Reload statespace for independent measurements.

                    // As the first timing is magnitudes off but the remaining ones yield
                    // consistent results (stdev 99 % smaller), we do an initial measurement
                    // we further will discard.
                    gen.generate(p, mch);
                    PredDbEntry result = gen.generate(p, mch);

                    dbEntries.get(p).add(result);
                }

                mch.close();
            } catch (LabelCreationException e) {
                e.printStackTrace();
            } catch (MachineAccessException e) {
                throw new RuntimeException(e);
            }
        });


        // mean/stdev
        Map<BPredicate, Map<Backend, Double[]>> stats = new HashMap<>();
        for (BPredicate pred : preds) {
            stats.put(pred, new HashMap<>());
            List<PredDbEntry> measures = dbEntries.get(pred);
            for (Backend b : backends) {
                List<Long> timings = measures.stream()
                        .map(e -> e.getAnswerArray(b)[0])
                        .filter(a -> ! (a.getAnswer().equals(Answer.ERROR)))
                        .map(a -> a.getTime(TimeUnit.MILLISECONDS))
                        .collect(Collectors.toList());

                if (!timings.isEmpty()) {
                    int numMeasures = timings.size();
                    assert numMeasures == sampSize;  // If this fails it means we need to prepare variable degrees of freedom.
                    double mean = timings.stream().mapToLong(l -> l).sum() * 1. / numMeasures;
                    StandardDeviation standardDeviation = new StandardDeviation(true); // Uses sample variance.
                    double stdev = standardDeviation.evaluate(timings.stream()
                            .mapToDouble(Long::doubleValue).toArray());

                    stats.get(pred).put(b, new Double[]{mean, stdev});
                }
            }
        }

        int degreesOfFreedom = sampSize - 1;
        TDistribution dist = new TDistribution(degreesOfFreedom);
        double tValue = dist.inverseCumulativeProbability(1-alpha/2.);

        // Per backend/predicate: calculate number of needed samples.
        Map<Backend, Double> minSamples = new HashMap<>();
        Map<Backend, List<Double>> samples = new HashMap<>();
        Map<Backend, List<Double>> nonErrSamples = new HashMap<>();
        Map<Backend, Map<BPredicate, Double>> predSamplesNeeded = new HashMap<>();
        for (Backend b : backends) {
            List<Double> sampleValues = new ArrayList<>();
            Map<BPredicate, Double> backendSamplesNeeded = new HashMap<>();
            predSamplesNeeded.put(b, backendSamplesNeeded);

            for (BPredicate p : preds) {
                if (stats.get(p).containsKey(b)) {
                    double mean = stats.get(p).get(b)[0];
                    double stdev = stats.get(p).get(b)[1];
                    double minSamplesNeeded = Math.pow(
                            tValue * stdev / error,
                            2.
                    );
                    sampleValues.add(minSamplesNeeded);
                    backendSamplesNeeded.put(p, minSamplesNeeded);
                }
            }
            minSamples.put(
                    b, sampleValues.stream()
                            .max(Double::compareTo)
                            .orElse(-1.));
        }

        System.out.println("Significance level (alpha): " + alpha);
        System.out.println("Allowed error: +/- " + error + " ms");
        System.out.println("Degrees of freedom (sample size minus one): " + degreesOfFreedom);
        System.out.println("Critical t-Value: " + tValue);
        for (Backend b : backends) {
            System.out.println("- " + b.toString() + ": "
                               + minSamples.get(b) + " samples");
        }
        System.out.println();

        int counter = 0;
        for (BPredicate p : preds) {
            counter++;
            System.out.println(counter + ". pred: " + p);

            /* Print out table for comprehensive overview */
            // Table header
            StringBuilder tableHeader = new StringBuilder("measure no.");
            for (Backend b : backends) {
                tableHeader.append("\t").append(b);
            }
            System.out.println(tableHeader);

            // Rows with samples
            List<PredDbEntry> measures = dbEntries.get(p);
            for (int s=0; s<measures.size(); s++) {
                PredDbEntry sample = measures.get(s);
                StringBuilder sampleRow = new StringBuilder(Integer.toString(s+1));
                for (Backend b : backends) {
                    sampleRow.append("\t");
                    TimedAnswer backendResult = sample.getResult(b);
                    Answer ans = backendResult.getAnswer();
                    if (ans.equals(Answer.TIMEOUT)) {
                        sampleRow.append('*');
                    } else if (ans.equals(Answer.ERROR)) {
                        sampleRow.append("**");
                    }
                    sampleRow.append(backendResult.getTime(TimeUnit.MILLISECONDS));
                }
                System.out.println(sampleRow);
            }

            // Statistics
            StringBuilder means = new StringBuilder("mean");
            StringBuilder stdevs = new StringBuilder("stdev");
            StringBuilder stderrs = new StringBuilder("stderr");
            StringBuilder samplesNeed = new StringBuilder("samples needed");
            for (Backend b : backends) {
                if (stats.get(p).containsKey(b)) {
                    double mean = stats.get(p).get(b)[0];
                    means.append('\t');
                    means.append(mean);
                    double stdev = stats.get(p).get(b)[1];
                    stdevs.append('\t');
                    stdevs.append(stdev);
                    double stderr = stdev / (Math.sqrt(sampSize));
                    stderrs.append('\t');
                    stderrs.append(stderr);
                    double need = predSamplesNeeded.get(b).get(p);
                    samplesNeed.append('\t');
                    samplesNeed.append(need);
                } else {
                    means.append('\t');
                    means.append("err");
                    stdevs.append('\t');
                    stdevs.append("err");
                    stderrs.append('\t');
                    stderrs.append("err");
                    samplesNeed.append('\t');
                    samplesNeed.append("err");
                }
            }
            System.out.println(means);
            System.out.println(stdevs);
            System.out.println(stderrs);
            System.out.println(samplesNeed);
            System.out.println();
        }


    }


    // TODO: Move parsing methods from DataCli and SamplingCli into shared utility class.
    List<Backend> parseBackends(CommandLine line) {
        if (line.hasOption('b')) {
            String[] backends = line.getOptionValues('b');
            boolean crossCreate = line.hasOption('x');
            return parseBackends(backends, crossCreate);
        } else if (line.hasOption('n')) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(PredDbEntry.DEFAULT_BACKENDS);
        }
    }

    List<Backend> parseBackends(String[] ids, boolean crossCreate) {
        List<Backend> backends = new ArrayList<>();
        for (String backend : ids) {
            Arrays.stream(BackendId.makeBackends(backend, crossCreate))
                    .forEach(backends::add);
        }
        return backends;
    }

    class MchPredPair {
        private final Path mch;
        private final BPredicate pred;

        public MchPredPair(Path mch, BPredicate pred) {
            this.mch = mch;
            this.pred = pred;
        }

        public Path getMch() {
            return mch;
        }

        public BPredicate getPred() {
            return pred;
        }
    }

}
