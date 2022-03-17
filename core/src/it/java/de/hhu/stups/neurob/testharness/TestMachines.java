package de.hhu.stups.neurob.testharness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Unified convenience access to the machines in src/it/resources/machines */
public class TestMachines {
    /** Full path description of test machine directory. */
    public static String TEST_MACHINE_DIR =
            getMachinePath("");

    public static String FORMULAE_GEN_MCH =
            getMachinePath("formulae_generation.mch");
    public static String FORMULAE_GEN_MCH_PREDICATE_FILE =
            "formulae_generation.predicates";

    public static String FEATURES_CHECK_MCH =
            getMachinePath("features_check.mch");
    public static String FEATURES_CHECK_JSON =
            getDbPath("json/features_check.json");
    /** Location of a JSON file which is not in valid JSON format */
    public static String FEATURES_CHECK_CORRUPT_JSON =
            getDbPath("json/features_check_corrupt.json");

    public static String EXAMPLE_BCM =
            getMachinePath("event-b/example/example.bcm");

    private static final Map<String, List<String>> expectedPredicates = new HashMap<>();

    /**
     * Generates the path to the src/it/resources/machines/machineName file.
     *
     * @param machineName Name of the machine without the "machine/"
     *         direcotry prefix.
     *
     * @return Correct path to machine resource at runtime.
     */
    public static String getMachinePath(String machineName) {
        Path directoryPath = TestResourceLoader.getResourcePath("machines");
        Path fullPath = directoryPath.resolve(machineName);
        return fullPath.toString();
    }
    /**
     * Generates the path to the src/it/resources/db/dbName file.
     *
     * @param dbName Name of the file without the "db/"
     *         direcotry prefix.
     *
     * @return Correct path to machine resource at runtime.
     */
    public static String getDbPath(String dbName) {
        Path directoryPath = TestResourceLoader.getResourcePath("db");
        Path fullPath = directoryPath.resolve(dbName);
        return fullPath.toString();
    }

    /**
     * Loads linewise the predicates from
     * src/it/resources/expected_predicates/listName file.
     * <p>
     * The loaded predicates are cached, so the file has not to be loaded each
     * time the list is accessed.
     *
     * @param listName
     *
     * @return
     */
    public static List<String> loadExpectedPredicates(String listName)
            throws IOException {
        if (expectedPredicates.containsKey(listName)) {
            return expectedPredicates.get(listName);
        }

        Path directoryPath = TestResourceLoader.getResourcePath("expected_predicates");
        Path fullPath = directoryPath.resolve(listName);

        Stream<String> lines = Files.lines(fullPath);
        List<String> predicates = lines.collect(Collectors.toList());
        expectedPredicates.put(listName, predicates);

        return predicates;

    }

}
