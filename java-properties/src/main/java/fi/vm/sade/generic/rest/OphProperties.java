package fi.vm.sade.generic.rest;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * Reads property values from classpath files, files in filesystem paths and system properties (-D).
 * Supports parameter replace (index & named) and url generation.
 * Collects configuration to be used at front (application code can configure separately which files are loaded and which system properties prefixes are used for front).
 */
public class OphProperties {
    public final PropertyLoadingConfig config = new PropertyLoadingConfig();
    public final PropertyLoadingConfig frontConfig = new PropertyLoadingConfig();
    public Properties ophProperties = null;
    public Properties frontProperties = null;

    public final Properties defaults = new Properties();
    public final Properties defaultOverrides = new Properties();

    class PropertyLoadingConfig {
        public List<String> classpathPaths = new ArrayList<String>();
        public List<String> filePaths = new ArrayList<String>();
        public List<String> systemPropertyFileKeys = new ArrayList<String>();

        public Properties load() {
            Properties dest = new Properties();
            for (String path : classpathPaths) {
                merge(dest, loadPropertiesFromResource(path));
            }
            for (String path : filePaths) {
                merge(dest, loadPropertiesFromPath(path));
            }
            Properties system = System.getProperties();
            for (String key : systemPropertyFileKeys) {
                if (system.containsKey(key)) {
                    for (String path : system.getProperty(key).split(".")) {
                        merge(dest, loadPropertiesFromPath(path));
                    }
                }
            }
            return dest;
        }
    }

    public OphProperties() {
        config.systemPropertyFileKeys.add("oph-properties");
        frontConfig.systemPropertyFileKeys.add("front-properties");
    }

    public OphProperties reload() {
        ophProperties = merge(new Properties(), config.load(), System.getProperties());
        merge(ophProperties, getPropertiesWithPrefix(ophProperties, "url."));
        frontProperties = merge(new Properties(),
                getPropertiesWithPrefix(ophProperties, "url.", "front."),
                frontConfig.load(),
                getPropertiesWithPrefix(System.getProperties(), "url.", "front."));
        return this;
    }

    synchronized public void ensureLoad() {
        if (ophProperties == null) {
            reload();
        }
    }

    private Properties getPropertiesWithPrefix(Properties props, String... prefixes) {
        Properties dest = new Properties();
        for (String prefix : prefixes) {
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    dest.setProperty(key.substring(prefix.length()), props.getProperty(key));
                }
            }
        }
        return dest;
    }

    private Properties loadPropertiesFromResource(String file) {
        return loadProperties(this.getClass().getResourceAsStream(file));
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

    public String require(String key, Object... params) {
        String value = ophProperties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("'" + key + "' not defined.");
        }
        return replaceParams(value, params);
    }

    public String getProperty(String key, Object... params) {
        String value = ophProperties.getProperty(key);
        if (value != null) {
            return replaceParams(value, params);
        }
        return null;
    }

    private String replaceParams(String url, Object... params) {
        for (int i = params.length; i > 0; i--) {
            Object param = params[i - 1];
            if (param instanceof Map) {
                Map paramMap = (Map) param;
                for (Object key : paramMap.keySet()) {
                    Object o = paramMap.get(key);
                    String value = enc(o);
                    String keyString = enc(key);
                    url = url.replace("$" + keyString, value);
                }
            } else {
                url = url.replace("$" + i, enc(param));
            }
        }
        return url;
    }

    private String enc(Object param) {
        return param == null ? "" : param.toString();
    }

    public String url(String key, Object... params) {
        return new UrlResolver().url(key, params);
    }

    public UrlResolver urls(Object... args) {
        Properties urlsConfig = new Properties();
        for (Object o : args) {
            if (o instanceof Map) {
                merge(urlsConfig, (Map) o);
            } else if (o instanceof String) {
                urlsConfig.put("baseUrl", o);
            }
        }
        return new UrlResolver(urlsConfig);
    }

    private static <D extends Map> D merge(D dest, Map... maps) {
        for (Map map : maps) {
            for (Object key : map.keySet()) {
                dest.put(key, map.get(key));
            }
        }
        return dest;
    }

    public class UrlResolver {
        private final Properties urlsConfig = new Properties();
        private boolean encode = true;

        public UrlResolver(Properties urlsConfig) {
            this();
            merge(this.urlsConfig, urlsConfig);
        }

        public UrlResolver() {
            ensureLoad();
        }

        private Object resolveConfig(String key) {
            return resolveConfig(key, null);
        }

        private Object resolveConfig(String key, String defaultValue) {
            for (Properties props : new Properties[]{urlsConfig, defaultOverrides, ophProperties, defaults}) {
                if (props.containsKey(key)) {
                    return props.get(key);
                }
            }
            return defaultValue;
        }

        public UrlResolver baseUrl(String baseUrl) {
            urlsConfig.put("baseUrl", baseUrl);
            return this;
        }

        public UrlResolver noEncoding() {
            encode = false;
            return this;
        }

        public String url(String key, Object... params) {
            Object o = resolveConfig(key);
            if (o == null) {
                throw new RuntimeException("'" + key + "' not defined.");
            }
            String url = replaceParams(o.toString(), params);
            Object baseUrl = resolveConfig(parseService(key) + ".baseUrl");
            if (baseUrl == null) {
                baseUrl = resolveConfig("baseUrl");
            }
            if (baseUrl != null) {
                url = joinUrl(baseUrl.toString(), url);
            }
            return url;
        }

        private String replaceParams(String url, Object... params) {
            String queryString = "";
            for (int i = params.length; i > 0; i--) {
                Object param = params[i - 1];
                if (param instanceof Map) {
                    Map paramMap = (Map) param;
                    for (Object key : paramMap.keySet()) {
                        Object o = paramMap.get(key);
                        String value = enc(o);
                        String keyString = enc(key);
                        String tmpUrl = url.replace("$" + keyString, value);
                        if (tmpUrl.equals(url)) {
                            if (queryString.length() > 0) {
                                queryString = queryString + "&";
                            } else {
                                queryString = "?";
                            }
                            queryString = queryString + keyString + "=" + value;
                        }
                        url = tmpUrl;
                    }
                } else {
                    url = url.replace("$" + i, enc(param));
                }
            }
            return url + queryString;
        }

        private String enc(Object key) {
            String s = key == null ? "" : key.toString();
            if (encode) {
                try {
                    s = URLEncoder.encode(s, "UTF-8")
                            .replaceAll("\\+", "%20")
                            .replaceAll("\\%21", "!")
                            .replaceAll("\\%27", "'")
                            .replaceAll("\\%28", "(")
                            .replaceAll("\\%29", ")")
                            .replaceAll("\\%7E", "~");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            return s;
        }

        private String joinUrl(String... urls) {
            if (urls.length == 0) {
                throw new RuntimeException("no arguments");
            }
            String url = null;
            for (String arg : urls) {
                if (url == null) {
                    url = arg;
                } else {
                    if (url.endsWith("/") || arg.startsWith("/")) {
                        url = url + arg;
                    } else {
                        url = url + "/" + arg;
                    }
                }
            }
            return url;
        }

        private String parseService(String key) {
            return key.substring(0, key.indexOf("."));
        }
    }
}
