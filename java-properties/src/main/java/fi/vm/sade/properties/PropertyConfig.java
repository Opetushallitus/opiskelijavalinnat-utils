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
    private List<String> classpathPaths = new ArrayList<String>();
    private List<String> filePaths = new ArrayList<String>();
    private List<String> systemPropertyFileKeys = new ArrayList<String>();

    public Properties load() {
        Properties dest = new Properties();
        for (String path : classpathPaths) {
            OphProperties.merge(dest, loadPropertiesFromResource(path));
        }
        for (String path : filePaths) {
            OphProperties.merge(dest, loadPropertiesFromPath(path));
        }
        Properties system = System.getProperties();
        for (String key : systemPropertyFileKeys) {
            if (system.containsKey(key)) {
                for (String path : system.getProperty(key).split(".")) {
                    OphProperties.merge(dest, loadPropertiesFromPath(path));
                }
            }
        }
        return dest;
    }

    public PropertyConfig addClassPathFile(String... files) {
        Collections.addAll(classpathPaths, files);
        return this;
    }

    public PropertyConfig addFile(String... files) {
        Collections.addAll(filePaths, files);
        return this;
    }

    public PropertyConfig addSystemKeyForFiles(String... keys) {
        Collections.addAll(systemPropertyFileKeys, keys);
        return this;
    }

    public Properties loadPropertiesFromResource(String file) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(file);
        if(resourceAsStream == null) {
            throw new RuntimeException("Resource file not found: "+ file);
        }
        return loadProperties(resourceAsStream);
    }

    private static Properties loadPropertiesFromPath(String path) {
        try {
            return loadProperties(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
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
