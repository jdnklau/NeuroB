package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.cli.BackendId;
import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.cli.Formats;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.PredicateTrainingGenerator;
import de.hhu.stups.neurob.training.generation.TrainingSetGenerator;
import de.hhu.stups.neurob.training.migration.PredicateDbMigration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataCli implements CliModule {

    private final Options options;

    public DataCli() {
        options = new Options();
        initOptions();
    }

    @Override
    public String getUsageInfo() {
        return
                "\n"
                + "       data -m SOURCE_DIR [FORMAT] -t TARGET_DIR TARGET_FORMAT\n"
                + "       data -g SOURCE_DIR -t TARGET_DIR TARGET_FORMAT [-[x][z]b BACKENDS]\n"
                + "\n";

    }

    @Override
    public String getHelpText() {
        StringWriter helpText = new StringWriter();
        PrintWriter helpPrinter = new PrintWriter(helpText);
        HelpFormatter formatter = new HelpFormatter();

        formatter.printUsage(helpPrinter, 80, getUsageInfo());
        formatter.printOptions(helpPrinter, 80, options, 1, 5);
        helpPrinter.println();

        helpPrinter.append(Formats.getFormatInfo());
        helpPrinter.println();

        helpPrinter.append(BackendId.backendInfo);
        helpPrinter.println();

        return helpText.toString();
    }

    void initOptions() {
        // Migration
        Option migrate = Option.builder("m")
                .longOpt("migrate")
                .hasArgs()
                .argName("PATH [FORMAT]")
                .desc("Source directory from which the data is migrated."
                      + " Database format defaults to jsonDb")
                .required()
                .build();

        // Generation
        Option generate = Option.builder("g")
                .longOpt("generate-from")
                .hasArg()
                .argName("PATH")
                .desc("Source directory from which the data is generated")
                .required()
                .build();

        OptionGroup modeGroup = new OptionGroup();
        modeGroup.addOption(migrate);
        modeGroup.addOption(generate);

        // Target
        Option target = Option.builder("t")
                .longOpt("target-dir")
                .numberOfArgs(2)
                .argName("PATH FORMAT")
                .desc("Directory and format into which the data is to be migrated")
                .required()
                .build();

        // BackendId
        Option cross = Option.builder("x")
                .longOpt("cross-options")
                .desc("If set, enumerates any combination of options per backend")
                .build();
        Option backends = Option.builder("b")
                .longOpt("backends")
                .hasArgs()
                .desc(
                        "BackendId to be accounted for"
                ).build();
        Option lazy = Option.builder("z")
                .longOpt("lazy")
                .desc("Data is generated lazily, aka already existent data is ignored").build();

        options.addOptionGroup(modeGroup);
        options.addOption(target);
        options.addOption(backends);
        options.addOption(cross);
        options.addOption(lazy);
    }

    @Override
    public void eval(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            // load common values
            Path targetDir = Paths.get(line.getOptionValues("t")[0]);
            TrainingDataFormat targetFormat = Formats.parseFormat(line.getOptionValues("t")[1]);

            // Switch between migration and generation
            if (line.hasOption("g")) {
                generate(line, targetDir, targetFormat);
            } else if (line.hasOption("m")) {

                Path sourceDir = Paths.get(line.getOptionValues("m")[0]);
                PredicateDbFormat sourceFormat =
                        (PredicateDbFormat) Formats.parseFormat(line.getOptionValues("m")[1]);

                // If the target format is a DbFormat as well, we need a different mode for migration
                if (targetFormat instanceof PredicateDbFormat) {
                    migrate(sourceDir, sourceFormat, targetDir, (PredicateDbFormat) targetFormat);
                } else {
                    System.out.println("Not yet implemented");
                }

            }

        } catch (ParseException e) {
            System.out.println("Unable to parse command line arguments: " + e);
        }
    }

    private void migrate(Path sourceDir, PredicateDbFormat sourceFormat, Path targetDir, PredicateDbFormat targetFormat) {
        try {
            new PredicateDbMigration(sourceFormat)
                    .migrate(sourceDir, targetDir, targetFormat);
        } catch (IOException e) {
            System.out.println("Unable to migrate data base: " + e);
        }
    }

    private void generate(CommandLine line, Path targetDir, TrainingDataFormat targetFormat) throws IOException {
        List<Backend> backends = new ArrayList<>();
        if (line.hasOption('b')) {
            boolean crossCreate = line.hasOption('x');


            for (String backend : line.getOptionValues('b')) {
                Arrays.stream(BackendId.makeBackends(backend, crossCreate))
                        .forEach(backends::add);
            }
        } else {
            backends = Arrays.asList(PredDbEntry.DEFAULT_BACKENDS);
        }

        Path sourceDir = Paths.get(line.getOptionValue("g"));

        // TODO: make use of target format
        JsonDbFormat format = new JsonDbFormat(backends.toArray(new Backend[0]));
        PredicateTrainingGenerator generator = new PredicateTrainingGenerator(
                (p,ss) -> p,
                format.getLabelGenerator(),
                format);

        generator.generateTrainingData(sourceDir, targetDir, line.hasOption('z'));

    }

    @Override
    public Options getOptions() {
        return options;
    }

}
