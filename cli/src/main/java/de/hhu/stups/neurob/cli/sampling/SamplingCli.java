package de.hhu.stups.neurob.cli.sampling;

import de.hhu.stups.neurob.cli.BackendId;
import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.LabelCreationException;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.core.labelling.DecisionTimings;
import de.hhu.stups.neurob.core.labelling.LabelGenerating;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateList;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class SamplingCli implements CliModule {

    private Options options;

    public SamplingCli() {
        options = new Options();
        initOptions();
    }

    @Override
    public String getUsageInfo() {
        return "sampling -f MACHINE_FILE -a ALPHA -e ERROR -s SAMPLING_SIZE\n";
    }

    private void initOptions() {
        Option mchFile = Option.builder("f")
                .longOpt("file")
                .hasArg()
                .argName("MACHINE_FILE")
                .desc("Calculate confidence interval over generated predicates from the given machine file.")
                .required()
                .build();

        Option alpha = Option.builder("a")
                .longOpt("alpha")
                .hasArg()
                .argName("ALPHA")
                .desc("Alpha-value for confidence level: 0.1 correlates to 90 % confidence.")
                .required()
                .build();

        Option error = Option.builder("e")
                .longOpt("error")
                .hasArg()
                .argName("ERROR")
                .desc("Allowed degrees of freedom; results in an allowed error of +/- 0.5*e %.")
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

        options.addOption(mchFile);
        options.addOption(alpha);
        options.addOption(error);
        options.addOption(samplingSize);
        options.addOption(backends);
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
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            Path listFile = Paths.get(line.getOptionValue('f'));
            double alpha = Double.parseDouble(line.getOptionValue('a'));
            double error = Double.parseDouble(line.getOptionValue('e'));
            int sampSize = Integer.parseInt(line.getOptionValue('s'));
            List<Backend> backends = parseBackends(line);

            calculateConfidence(listFile, alpha, error, sampSize, backends);

        } catch (ParseException e) {
            System.out.println("Unable to get command line arguments: " + e);
        }
    }

    void calculateConfidence(
            Path file,
            double alpha, double error, int sampSize,
            List<Backend> backends) {

        try {
            // Generate samples first.
            MachineAccess mch = new MachineAccess(file);
            PredicateCollection pc = new PredicateCollection(mch);

            List<BPredicate> preds = new ArrayList<>();
//            preds.addAll(FormulaGenerator.enablingAnalysis(pc));
            preds.addAll(FormulaGenerator.invariantConstrains(pc));

            System.out.println("Gathered " + preds.size() + " predicates.");

            // Gather runtimes
            Map<BPredicate, List<PredDbEntry>> dbEntries = new HashMap<>();
            PredDbEntry.Generator gen =
                    new PredDbEntry.Generator(1, backends.toArray(new Backend[0]));
            preds.forEach(p -> {
                try {
                    dbEntries.put(p, new ArrayList<>());
                    for (int i = 0; i <= sampSize; i++) {
                        // skip first measurement, as it takes longer than all the others.
                        PredDbEntry result = gen.generate(p, mch);
                        if (i > 0){
                            dbEntries.get(p).add(result);
                        }
                    }
                } catch (LabelCreationException e) {
                    e.printStackTrace();
                }
            });

            mch.close();

            // mean/stdev
            Map<BPredicate, Map<Backend, Double[]>> stats = new HashMap<>();
            Map<BPredicate, Map<Backend, Double[]>> nonErrStats = new HashMap<>();
            for (BPredicate pred : preds) {
                stats.put(pred, new HashMap<>());
                nonErrStats.put(pred, new HashMap<>());
                List<PredDbEntry> measures = dbEntries.get(pred);
                for (Backend b : backends) {
                    List<Long> timings = measures.stream()
                            .map(e -> e.getAnswerArray(b)[0])
                            .map(a -> a.getTime(TimeUnit.MILLISECONDS))
                            .collect(Collectors.toList());

                    List<Long> nonErrTimings = measures.stream()
                            .map(e -> e.getAnswerArray(b)[0])
                            .filter(a -> Answer.isSolvable(a.getAnswer()) || a.getAnswer().equals(Answer.UNKNOWN))
                            .map(a -> a.getTime(TimeUnit.MILLISECONDS))
                            .collect(Collectors.toList());

                    double mean = timings.stream().mapToLong(l -> l).sum() * 1. / sampSize;
                    StandardDeviation standardDeviation = new StandardDeviation(true);
                    double stdev = standardDeviation.evaluate(timings.stream()
                            .mapToDouble(Long::doubleValue).toArray());
                    System.out.println("");

                    stats.get(pred).put(b, new Double[]{mean, stdev});

                    mean = nonErrTimings.stream().mapToLong(l -> l).sum() * 1. / nonErrTimings.size();
                    standardDeviation = new StandardDeviation(true);
                    stdev = standardDeviation.evaluate(nonErrTimings.stream()
                            .mapToDouble(Long::doubleValue).toArray());
                    nonErrStats.get(pred).put(b, new Double[]{mean, stdev});
                }
            }

            TDistribution dist = new TDistribution(error);
            double tValue = dist.inverseCumulativeProbability(alpha);

            // Per backend/predicate: calculate number of needed samples.
            Map<Backend, Double> minSamples = new HashMap<>();
            Map<Backend, Double> nonErrMinSamples = new HashMap<>();
            for (Backend b : backends) {
                List<Double> sampleValues = new ArrayList<>();
                List<Double> nonErrSampleValues = new ArrayList<>();
                for (BPredicate p : preds) {
                    double mean = stats.get(p).get(b)[0];
                    double stdev = stats.get(p).get(b)[1];
                    double minSamplesNeeded = Math.pow(
                            tValue * stdev / ((error / 200.) * mean),
                            2.
                    );
                    sampleValues.add(minSamplesNeeded);

                    mean = nonErrStats.get(p).get(b)[0];
                    stdev = nonErrStats.get(p).get(b)[1];
                    minSamplesNeeded = Math.pow(
                            tValue * stdev / ((error / 200.) * mean),
                            2.
                    );
                    nonErrSampleValues.add(minSamplesNeeded);

                }
                minSamples.put(
                        b, sampleValues.stream()
                                .max(Double::compareTo)
                                .orElse(-1.));
                nonErrMinSamples.put(
                        b, nonErrSampleValues.stream()
                                .filter(d -> !Double.isNaN(d))
                                .max(Double::compareTo)
                                .orElse(-1.));
            }

            System.out.println("Alpha: " + alpha);
            System.out.println("Degrees of Freedom: " + error);
            System.out.println("t-Value: " + tValue);
            for (Backend b : backends) {
                System.out.println("- " + b.getName() + ": "
                                   + minSamples.get(b) + " samples ("
                                   + nonErrMinSamples.get(b) + " samples for non-error)");
            }
            System.out.println();

            int counter = 0;
            for (BPredicate p : preds) {
                counter++;
                System.out.print(counter + ". pred: ");
                for (Backend b : backends) {
                    double mean = stats.get(p).get(b)[0];
                    double stdev = stats.get(p).get(b)[1];
                    System.out.print(b.getName() + ": mean " + mean + ", stdev " + stdev + "; ");
                }
                System.out.println();
            }

        } catch (MachineAccessException e) {
            e.printStackTrace();
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

}
