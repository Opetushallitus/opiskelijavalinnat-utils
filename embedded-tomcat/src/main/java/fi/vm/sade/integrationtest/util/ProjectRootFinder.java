package fi.vm.sade.integrationtest.util;

import java.io.File;
import java.io.IOException;

public class ProjectRootFinder {
    public static File findProjectRoot() {
        try {
            return findRoot(new File(".").getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File findRoot(final File currentDirectory) {
        if (!pomExists(currentDirectory) || parentPomExists(currentDirectory)) {
            return findRoot(currentDirectory.getParentFile());
        }
        return currentDirectory;
    }

    private static boolean parentPomExists(File currentDirectory) {
        final File parent = currentDirectory.getParentFile();
        if (parent != null) {
            return pomExists(parent) || parentPomExists(parent);
        }
        return false;
    }

    private static boolean pomExists(File currentDirectory) {
        return new File(currentDirectory, "pom.xml").exists();
    }
}