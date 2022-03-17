package de.hhu.stups.neurob.testharness;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestResourceLoader {

    public static Path getResourcePath(String resourceName) {

        String fullPath = TestResourceLoader.class.getClassLoader()
                .getResource(resourceName)
                .getFile();

        return Paths.get(fullPath);
    }
}
