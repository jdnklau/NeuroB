package de.hhu.stups.neurob.cli.solving;

import de.hhu.stups.neurob.cli.BackendId;
import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SolvingCli implements CliModule {
    final Options options;

    public SolvingCli() {
        options = new Options();
        initOptions();
    }

    private void initOptions() {
        Option backends = Option.builder("b")
                .longOpt("backends")
                .hasArgs()
                .desc("BackendId to be accounted for.")
                .build();

        Option predicate = Option.builder("p")
                .longOpt("predicate")
                .hasArg()
                .desc("Predicate to solve")
                .build();

        Option probHome = Option.builder("h")
                .longOpt("prob-home")
                .hasArg()
                .argName("PATH")
                .desc("If set, uses the ProB cli located at the given path.")
                .optionalArg(true)
                .build();

        options.addOption(predicate);
        options.addOption(backends);
        options.addOption(probHome);
    }

    @Override
    public String getUsageInfo() {
        return
                /*use:*/ "solving [-b BACKENDS] -p PREDICATE";

    }

    @Override
    public String getHelpText() {
        StringWriter helpText = new StringWriter();
        PrintWriter helpPrinter = new PrintWriter(helpText);
        HelpFormatter formatter = new HelpFormatter();

        formatter.printUsage(helpPrinter, 80, getUsageInfo());
        helpPrinter.println();
        helpPrinter.println("options:");
        formatter.printOptions(helpPrinter, 80, options, 1, 5);
        helpPrinter.println();

        helpPrinter.append(BackendId.backendInfo);
        helpPrinter.println();

        return helpText.toString();
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

            // Check for ProB home
            if (line.hasOption("h")) {
                setProBHomeFromOption(line, "h");
            }

            List<Backend> backends = parseBackends(line);
            BPredicate pred = BPredicate.of(line.getOptionValue('p'));

            for (Backend b : backends) {
                MachineAccess mch = BMachine.EMPTY.spawnMachineAccess();

                TimedAnswer timedAnswer = b.solvePredicate(pred, mch);
                System.out.println(b);
                System.out.print("->  ");
                System.out.print("predicate is ");
                System.out.print(timedAnswer.getAnswer());
                System.out.print(", solved in ");
                System.out.print(timedAnswer.getTime(TimeUnit.MILLISECONDS));
                System.out.println(" ms");
                mch.close();
            }


        } catch (ParseException e) {
            System.out.println("Unable to get command line arguments: " + e);
        }
    }

    static void setProBHomeFromOption(CommandLine line, String fromOption) {
        String probHome = line.getOptionValue(fromOption);
        System.setProperty("prob.home", probHome);
    }

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
