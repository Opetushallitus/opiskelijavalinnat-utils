package fi.vm.sade.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class PropertyConfig {
    private class PropertyFile {
        final String path;
        final boolean throwError;

        private PropertyFile(String path, boolean throwError) {
            this.path = path;
            this.throwError = throwError;
        }
    }
    private List<PropertyFile> filePaths = new ArrayList<PropertyFile>();
    private List<String> systemPropertyFileKeys = new ArrayList<String>();

    public Properties load() {
        Properties dest = new Properties();
        for (PropertyFile file : filePaths) {
            OphProperties.merge(dest, loadPropertiesFromPath(file.path, file.throwError));
        }
        Properties system = System.getProperties();
        for (String key : systemPropertyFileKeys) {
            if (system.containsKey(key)) {
                for (String path : system.getProperty(key).split(",")) {
                    OphProperties.merge(dest, loadPropertiesFromPath(path, true));
                }
            }
        }
        return dest;
    }

    public PropertyConfig addFiles(String... paths) {
        for (String path : paths) {
            filePaths.add(new PropertyFile(path, true));
        }
        return this;
    }

    public PropertyConfig addOptionalFiles(String... paths) {
        for (String path : paths) {
            filePaths.add(new PropertyFile(path, true));
        }
        return this;
    }

    public PropertyConfig addSystemKeyForFiles(String... keys) {
        Collections.addAll(systemPropertyFileKeys, keys);
        return this;
    }

    private Properties loadPropertiesFromPath(String path, boolean throwError) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(path);
        if(resourceAsStream == null) {
            try {
                resourceAsStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                if(throwError) {
                    throw new RuntimeException("Could not load properties from " + path, e);
                } else {
                    System.out.println("Could not load properties from " + path + ". It was marked optional, this is not an error.");
                    return new Properties();
                }
            }
        }
        return loadProperties(resourceAsStream);
    }

    private static Properties loadProperties(InputStream inputStream) {
        try {
            final Properties properties = new Properties();
            try {
                properties.load(inputStream);
                return properties;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
