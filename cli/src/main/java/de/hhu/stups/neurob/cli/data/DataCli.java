package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.migration.PredicateDbMigration;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;
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

public class DataCli implements CliModule {

    private final Options options;

    public DataCli() {
        options = new Options();
        initOptions();
    }

    @Override
    public String getUsageInfo() {
        return
                "data -m SOURCE_DIR [FORMAT] -t TARGET_DIR TARGET_FORMAT\n"
                + "       data -g SOURCE_DIR -t TARGET_DIR TARGET_FORMAT\n"
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

        helpPrinter.append("Supported formats:\n"
                + " - jsonDb      Predicate data base of json files\n"
                + " - pdump       Legacy Predicate Dump data base format\n\n");

        return helpText.toString();
    }

    void initOptions() {
        // Migration
        Option migrate = Option.builder("m")
                .longOpt("migrate")
                .hasArgs()
                .desc("Source directory from which the data is migrated."
                      + " Database format defaults to jsonDb")
                .required()
                .build();

        // Generation
        Option generate = Option.builder("g")
                .longOpt("generate-from")
                .hasArg()
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
                .desc("Directory and format into which the data is to be migrated")
                .required()
                .build();


        options.addOptionGroup(modeGroup);
        options.addOption(target);
    }

    @Override
    public void eval(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            // load common values
            Path targetDir = Paths.get(line.getOptionValues("t")[0]);
            TrainingDataFormat targetFormat = parseFormat(line.getOptionValues("t")[1]);

            // Switch between migration and generation
            if (line.hasOption("g")) {
                System.err.println("generation NYI");
            } else if (line.hasOption("m")) {

                Path sourceDir = Paths.get(line.getOptionValues("m")[0]);
                PredicateDbFormat sourceFormat =
                        (PredicateDbFormat) parseFormat(line.getOptionValues("m")[1]);

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

    private TrainingDataFormat parseFormat(String t) {
        if ("jsonDb".equals(t)) {
            return new JsonDbFormat();
        } else if ("pdump".equals(t)) {
            return new PredicateDumpFormat();
        }
        // TODO Implement more
        return null;
    }

    @Override
    public Options getOptions() {
        return options;
    }

}
