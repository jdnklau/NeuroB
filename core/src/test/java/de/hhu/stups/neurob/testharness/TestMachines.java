package de.hhu.stups.neurob.testharness;

/** Unified convenience access to the machines in src/test/resources/machines */
public class TestMachines {
    public static String FORMULAE_GEN_MCH =
            getMachinePath("formulae_generation.mch");
    public static String FEATURES_CHECK_MCH =
            getMachinePath("features_check.mch");

    /**
     * Generates the path to the src/test/resources/machines/machineName file.
     *
     * @param machineName Name of the machine without the "machine/"
     *         direcotry prefix.
     *
     * @return Correct path to machine resource at runtime.
     */
    public static String getMachinePath(String machineName) {
        String machineSubPath = "machines/" + machineName;
        return TestMachines.class.getClassLoader()
                .getResource(machineSubPath).getFile();
    }
}
