package de.hhu.stups.neurob.cli.data;

import de.hhu.stups.neurob.cli.BackendId;
import de.hhu.stups.neurob.cli.CliModule;
import de.hhu.stups.neurob.cli.formats.Formats;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.features.predicates.BAst109Reduced;
import de.hhu.stups.neurob.core.features.predicates.BAst110Features;
import de.hhu.stups.neurob.core.features.predicates.BAst115Features;
import de.hhu.stups.neurob.core.features.predicates.PredicateFeatureGenerating;
import de.hhu.stups.neurob.core.features.predicates.RawPredFeature;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.core.labelling.HealyTimings;
import de.hhu.stups.neurob.core.labelling.SettingsMultiLabel;
import de.hhu.stups.neurob.training.analysis.PredDbAnalysis;
import de.hhu.stups.neurob.training.analysis.PredicateDbAnalyser;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.db.PredicateDbFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.generation.PredicateTrainingGenerator;
import de.hhu.stups.neurob.training.generation.util.FormulaGenerator;
import de.hhu.stups.neurob.training.migration.PredicateDbMigration;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataCli implements CliModule {

    final Options options;

    public DataCli() {
        options = new Options();
        initOptions();
    }

    @Override
    public String getUsageInfo() {
        return
                /*use:*/ "data -m SOURCE_DIR FORMAT -t TARGET_DIR TARGET_FORMAT TARGET_FEATURES TARGET_LABELS\n"
                + "       data -g SOURCE_DIR -t TARGET_DIR TARGET_FORMAT [OPTIONS] [-s SAMPLING_SIZE] [-[x][z]b BACKENDS | -n]\n"
                + "       data -a SOURCE_DIR FORMAT [-c THREADS] [-[x]b BACKENDS] [-f FILE_NAME]\n"
                + "       data -p SOURCE_DIR -o TARGET_PATH [- THREADS]\n";

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

        helpPrinter.append(Formats.getFormatInfo());
        helpPrinter.println();

        helpPrinter.append(Features.getFeatureInfo());
        helpPrinter.println();

        helpPrinter.append(Labels.getLabelInfo());
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
                .numberOfArgs(2)
                .argName("PATH [FORMAT]")
                .desc("Source directory from which the data is migrated.")
                .required()
                .build();

        // Generation
        Option generate = Option.builder("g")
                .longOpt("generate-from")
                .hasArg()
                .argName("PATH")
                .desc("Source directory from which the data is generated.")
                .required()
                .build();

        // Analysis
        Option analyse = Option.builder("a")
                .longOpt("analyse")
                .numberOfArgs(2)
                .argName("PATH FORMAT")
                .desc("Path to database to analyse and corresponding format.")
                .required()
                .build();

        // Analysis
        Option predsOnly = Option.builder("p")
                .longOpt("predicates")
                .hasArg()
                .argName("PATH")
                .desc("Source directory from which the predicates are generated/extracted.")
                .required()
                .build();

        OptionGroup modeGroup = new OptionGroup();
        modeGroup.addOption(migrate);
        modeGroup.addOption(generate);
        modeGroup.addOption(analyse);
        modeGroup.addOption(predsOnly);

        // Target
        Option target = Option.builder("t")
                .longOpt("target-dir")
                .numberOfArgs(4)
                .argName("PATH FORMAT FEATURES LABELS")
                .desc("Directory and format into which the generated data is placed.")
                .build();

        Option output = Option.builder("o")
                .longOpt("output-file")
                .hasArg()
                .argName("PATH")
                .desc("Path to the output file.")
                .build();

        Option examplesDir = Option.builder("e")
                .longOpt("examples-dir")
                .numberOfArgs(1)
                .argName("PATH")
                .desc("Path to the ProB Examples or other data source. Mandatory if translating database into features.")
                .build();

        // Number of cores
        Option cores = Option.builder("c")
                .longOpt("threats")
                .hasArg()
                .argName("THREADS")
                .desc("Number of threads to be run in parallel. "
                      + "Defaults to number of processors minus one.")
                .build();

        Option exclude = Option.builder("i")
                .longOpt("exclude-list")
                .hasArg()
                .argName("EXCLUDE_FILE")
                .desc("Path to a text file, linewise containing sources to be ignored during "
                      + "data generation.")
                .build();

        Option countFile = Option.builder("f")
                .longOpt("count-file")
                .hasArg()
                .argName("FILE")
                .desc("If set, analysis outputs a csv file containing the number of predicates "
                      + "for each machine.")
                .build();

        Option samplingSize = Option.builder("s")
                .longOpt("sampling-size")
                .hasArg()
                .argName("SAMPLING_SIZE")
                .desc("Number of times each sample is to be measured. Defaults to 1.")
                .build();

        Option noBackends = Option.builder("n")
                .longOpt("no-backends")
                .desc("Indicates that no backends shall be used instead of the default ones.")
                .build();

        // BackendId
        Option cross = Option.builder("x")
                .longOpt("cross-options")
                .desc("If set, enumerates any combination of options per backend.")
                .build();
        Option backends = Option.builder("b")
                .longOpt("backends")
                .hasArgs()
                .desc("BackendId to be accounted for.")
                .build();
        Option lazy = Option.builder("z")
                .longOpt("lazy")
                .desc("Data is generated lazily, i.e. already existent data is ignored.").build();

        options.addOptionGroup(modeGroup);
        options.addOption(target);
        options.addOption(countFile);
        options.addOption(examplesDir);
        options.addOption(backends);
        options.addOption(noBackends);
        options.addOption(cross);
        options.addOption(lazy);
        options.addOption(cores);
        options.addOption(exclude);
        options.addOption(samplingSize);
        options.addOption(output);
    }

    @Override
    public void eval(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            // Check for analysis
            if (line.hasOption("a")) {
                Path sourceDir = parseSourceDirectory(line, "a");

                List<Backend> backends = parseBackends(line);

                PredicateDbFormat dbFormat = (PredicateDbFormat) parseFormat(line, "a");

                if (line.hasOption("f")) {
                    Path countFile = parseSourceDirectory(line, "f");
                    createCountFile(sourceDir, countFile, dbFormat, backends);
                } else {
                    analyse(sourceDir, dbFormat, backends);
                }
            } else if (line.hasOption("g")) {
                generate(line, parseTargetDirectory(line));

            } else if (line.hasOption("m")) {
                Backend[] backends = parseBackends(line).toArray(new Backend[]{});
                MigrationFormat migration = parseMigration(line, "t", backends);
                TrainingDataFormat targetFormat = migration.getFormat();

                Path sourceDir = parseSourceDirectory(line, "m");
                PredicateDbFormat sourceFormat =
                        (PredicateDbFormat) parseFormat(line, "m");

                // If the target format is a DbFormat as well, we need a different mode for migration
                if (targetFormat instanceof PredicateDbFormat) {
                    migrate(sourceDir, sourceFormat, parseTargetDirectory(line),
                            (PredicateDbFormat) targetFormat);
                } else {
                    Path targetDir = parseSourceDirectory(line, "t");
                    Path genDir = parseSourceDirectory(line, "e");

                    migrateFeatures(sourceDir, sourceFormat, targetDir, genDir, migration);

                }

            } else if (line.hasOption("p")) {
                Path sourceDir = parseSourceDirectory(line, "p");
                Path targetDir = parseSourceDirectory(line, "o");

                createPredicateList(line, sourceDir, targetDir);
            }

        } catch (ParseException e) {
            System.out.println("Unable to get command line arguments: " + e);
        }
    }

    private void createCountFile(Path sourceDir, Path countFile, TrainingDataFormat format, List<Backend> backends) {
        if (format instanceof PredicateDbFormat) {
            PredicateDbFormat<PredDbEntry> dbFormat = (PredicateDbFormat<PredDbEntry>) format;
            PredicateDbAnalyser analyser = new PredicateDbAnalyser(dbFormat);
            try {
                Path parent = countFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                BufferedWriter writer = Files.newBufferedWriter(countFile);
                writer.write("count,path\n");
                Files.walk(sourceDir)
                        .filter(p -> p.toString().endsWith(format.getFileExtension()))
                        .forEach(p -> writeFileInfo(sourceDir, p, writer, analyser));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Analysis for non-pred-db formats not yet implemented.");
        }
    }

    private void writeFileInfo(Path source, Path file, Writer countFile, PredicateDbAnalyser analyser) {
        try {
            PredDbAnalysis analysis = analyser.analyse(file);
            countFile.write(analysis.getPredCount().toString());
            countFile.write(',');
            countFile.write(source.relativize(file).toString());
            countFile.write('\n');
            countFile.flush();
        } catch (IOException e) {
            System.err.println("Unable to gather info for " + file);
        }
    }

    private void analyse(Path sourceDir, TrainingDataFormat format, List<Backend> backends) {
        if (format instanceof PredicateDbFormat) {
            PredicateDbFormat<PredDbEntry> dbFormat = (PredicateDbFormat<PredDbEntry>) format;
            PredicateDbAnalyser analyser = new PredicateDbAnalyser(dbFormat);
            try {
                System.out.println(analyser.analyse(sourceDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Analysis for non-pred-db formats not yet implemented.");
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

    private void migrateFeatures(Path sourceDir, PredicateDbFormat sourceFormat, Path targetDir, Path genSource,
            MigrationFormat migration) {
        try {

            new PredicateDbMigration(sourceFormat)
                    .migrate(sourceDir, targetDir, genSource,
                            migration.getFeatures(), migration.getLabels(), migration.getFormat());
        } catch (IOException e) {
            System.out.println("Unable to migrate data base: " + e);
        } catch (Exception e) {
            System.out.println("Unable recognise features or labels: " + e);
        }
    }

    private void createPredicateList(CommandLine line, Path sourceDir, Path targetDir) {
        PredicateTrainingGenerator generator = new PredicateTrainingGenerator(
                (p, ss) -> p,
                new PredDbEntry.Generator(1),
                new JsonDbFormat(new Backend[]{}));

        setGenerationRules(generator);

        int numThreads = parseCores(line);

        Collection<Path> excludes = getExcludes(line);

        ForkJoinPool threadPool = new ForkJoinPool(numThreads);
        try {
            threadPool.submit(
                    () -> generator.generateTrainingData(
                            sourceDir,
                            targetDir,
                            line.hasOption('z'),
                            excludes))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void setGenerationRules(PredicateTrainingGenerator generator) {
        generator.setGenerationRules(
                FormulaGenerator::assertions,
                FormulaGenerator::enablingAnalysis,
                FormulaGenerator::extendedPreconditionFormulae,
                FormulaGenerator::invariantConstrains,
                FormulaGenerator::invariantPreservationFormulae,
                FormulaGenerator::multiPreconditionFormulae,
                FormulaGenerator::weakestPreconditionFormulae
        );
    }

    private void generate(CommandLine line, Path targetDir) throws IOException {
        List<Backend> backends = parseBackends(line);

        Path sourceDir = Paths.get(line.getOptionValue("g"));

        int samplingSize = line.hasOption("s")
                ? Integer.parseInt(line.getOptionValue("s"))
                : 1;

        TrainingDataFormat format = parseFormat(line, "t");

        // FIXME: Plain training data not supported yet
        if (!(format instanceof PredicateDbFormat)) {
            System.out.println("Non-database formats not yet supported.");
            return;
        }

        PredicateDbFormat dbFormat = (PredicateDbFormat) format;

        PredicateTrainingGenerator generator = new PredicateTrainingGenerator(
                (p, ss) -> p,
                new PredDbEntry.Generator(samplingSize, backends.toArray(new Backend[0])),
                samplingSize,
                dbFormat);

        setGenerationRules(generator);

        int numThreads = (line.hasOption("c"))
                ? Integer.parseInt(line.getOptionValue("c"))
                : Runtime.getRuntime().availableProcessors() - 1;

        Collection<Path> excludes = getExcludes(line);

        ForkJoinPool threadPool = new ForkJoinPool(numThreads);
        try {
            threadPool.submit(
                    () -> generator.generateTrainingData(
                            sourceDir,
                            targetDir,
                            line.hasOption('z'),
                            excludes))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Parses the format from the specified command line option.
     * <p>
     * Per convention, the identifier of the format resides in index position 1
     * of the arguments to that option
     *
     * @param line
     * @param fromOption
     *
     * @return
     *
     * @throws Exception
     */
    TrainingDataFormat parseFormat(CommandLine line, String fromOption) {
        String[] arguments = line.getOptionValues(fromOption);
        String formatId = arguments[1];

        List<Backend> backends = parseBackends(line);

        return Formats.parseFormat(formatId, 0, 0, backends); // TODO: Where is this called?
    }

    /**
     * Parses the list of backends to use from the command line.
     * <p>
     * This represents the -b option.
     *
     * @param line
     *
     * @return
     */
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

    /**
     * Parses the list of backends to use from the ids specified in the command line.
     * Might cross-create the preferences.
     * <p>
     * This represents the -b option.
     *
     * @param ids Commandline entries given.
     * @param crossCreate Whether the preferences shall be mixed.
     *
     * @return
     */
    List<Backend> parseBackends(String[] ids, boolean crossCreate) {
        List<Backend> backends = new ArrayList<>();
        for (String backend : ids) {
            Arrays.stream(BackendId.makeBackends(backend, crossCreate))
                    .forEach(backends::add);
        }
        return backends;
    }

    Path parseSourceDirectory(CommandLine line, String fromOption) {
        if (line.hasOption(fromOption)) {
            return Paths.get(line.getOptionValues(fromOption)[0]);
        }
        return null;
    }

    MigrationFormat parseMigration(CommandLine line, String fromOption, Backend[] backends) {
        if (line.hasOption(fromOption)) {
            try {
                String[] lineOptions = line.getOptionValues(fromOption);
                return new MigrationFormat(lineOptions[2], lineOptions[3], lineOptions[1], backends);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    Path parseTargetDirectory(CommandLine line) {
        if (line.hasOption("t")) {
            return Paths.get(line.getOptionValues("t")[0]);
        }
        return null;
    }

    int parseCores(CommandLine line) {
        int numThreads = (line.hasOption("c"))
                ? Integer.parseInt(line.getOptionValue("c"))
                : Runtime.getRuntime().availableProcessors() - 1;
        return numThreads;
    }

    private Collection<Path> getExcludes(CommandLine line) {
        if (line.hasOption('i')) {
            Path excludes = Paths.get(line.getOptionValue('i'));
            try (Stream<String> lines = Files.lines(excludes)) {
                return lines
                        .filter(s -> !s.isEmpty())
                        .map(Paths::get)
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HashSet<>();
    }

    @Override
    public Options getOptions() {
        return options;
    }

}
