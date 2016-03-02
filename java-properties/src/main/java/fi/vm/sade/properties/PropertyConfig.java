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
    private List<String> filePaths = new ArrayList<String>();
    private List<String> systemPropertyFileKeys = new ArrayList<String>();

    public Properties load() {
        Properties dest = new Properties();
        for (String path : filePaths) {
            OphProperties.merge(dest, loadPropertiesFromPath(path));
        }
        Properties system = System.getProperties();
        for (String key : systemPropertyFileKeys) {
            if (system.containsKey(key)) {
                for (String path : system.getProperty(key).split(",")) {
                    OphProperties.merge(dest, loadPropertiesFromPath(path));
                }
            }
        }
        return dest;
    }

    public PropertyConfig addFile(String... files) {
        Collections.addAll(filePaths, files);
        return this;
    }

    public PropertyConfig addSystemKeyForFiles(String... keys) {
        Collections.addAll(systemPropertyFileKeys, keys);
        return this;
    }

    private Properties loadPropertiesFromPath(String path) {
        InputStream resourceAsStream = this.getClass().getResourceAsStream(path);
        if(resourceAsStream == null) {
            try {
                resourceAsStream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not load properties from " + path, e);
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
