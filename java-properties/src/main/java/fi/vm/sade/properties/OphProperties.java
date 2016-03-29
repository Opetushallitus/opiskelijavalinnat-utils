package fi.vm.sade.properties;

import java.util.*;

import static fi.vm.sade.properties.UrlUtils.joinUrl;

/**
 * Reads property values from classpath files, files in filesystem paths and system properties (-D).
 * Supports parameter replace (index & named) and url generation.
 * Collects configuration to be used at front (application code can configure separately which files are loaded and which system properties prefixes are used for front).
 */
public class OphProperties {
    public final PropertyConfig config = new PropertyConfig();
    public final PropertyConfig frontConfig = new PropertyConfig();
    public Properties ophProperties = null;
    public Properties frontProperties = null;

    public final Properties defaults = new Properties();
    public final Properties overrides = new Properties();
    private final ParamReplacer replacer = new ParamReplacer();
    private boolean debug = System.getProperty("OphProperties.debug", null) != null;

    public OphProperties(String... files) {
        config.addSystemKeyForFiles("oph-properties");
        frontConfig.addSystemKeyForFiles("front-properties");
        addFiles(files);
    }

    public OphProperties addFiles(String... files) {
        config.addFiles(files);
        return reload();
    }

    public OphProperties addOptionalFiles(String... files) {
        config.addOptionalFiles(files);
        return reload();
    }

    public OphProperties reload() {
        try {
            ophProperties = merge(new Properties(), config.load(), System.getProperties());
            merge(ophProperties, getPropertiesWithPrefix(ophProperties, "url."));
            frontProperties = merge(new Properties(),
                    getPropertiesWithPrefix(ophProperties, "url.", "front."),
                    frontConfig.load(),
                    getPropertiesWithPrefix(System.getProperties(), "url.", "front."));
            return this;
        } catch(Exception e) {
            debug("reload threw exception:", e.getMessage());
            throw e;
        }
    }

    private synchronized void ensureLoad() {
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

    public String require(String key, Object... params) {
        String value = ophProperties.getProperty(key);
        if (value == null) {
            debug(key, "not found. Throw exception");
            throw new RuntimeException("'" + key + "' not defined.");
        } else {
            value = replacer.replaceParams(value, convertParams(params));
            debug(key, "->", value);
        }
        return value;
    }

    public String getProperty(String key, Object... params) {
        String value = ophProperties.getProperty(key);
        if (value == null) {
            debug(key, "not found. Returning null");
        } else {
            value = replacer.replaceParams(value, convertParams(params));
            debug(key, "->", value);
        }
        return value;
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

    public static <D extends Map> D merge(D dest, Map... maps) {
        for (Map map : maps) {
            for (Object key : map.keySet()) {
                dest.put(key, map.get(key));
            }
        }
        return dest;
    }

    // extension point for other programming languages. Insert code which converts Maps, case classes etc to Java Maps
    public Object[] convertParams(Object... params) {
        return params;
    }

    public class UrlResolver extends ParamReplacer {
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
            for (Properties props : new Properties[]{urlsConfig, overrides, ophProperties, defaults}) {
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
            String url = replaceParams(o.toString(), convertParams(params));
            Object baseUrl = resolveConfig(parseService(key) + ".baseUrl");
            if (baseUrl == null) {
                baseUrl = resolveConfig("baseUrl");
            }
            if (baseUrl != null) {
                url = joinUrl(baseUrl.toString(), url);
            }
            debug("url:", key, "->", url);
            return url;
        }

        @Override
        String extraParam(String queryString, String keyString, String value) {
            if (queryString.length() > 0) {
                queryString = queryString + "&";
            } else {
                queryString = "?";
            }
            return queryString + keyString + "=" + value;
        }

        @Override
        String enc(Object key) {
            String s = key == null ? "" : key.toString();
            if (encode) {
                s = UrlUtils.encode(s);
            }
            return s;
        }

        private String parseService(String key) {
            return key.substring(0, key.indexOf("."));
        }
    }

    public OphProperties debugMode() {
        debug = true;
        return this;
    }

    private void debug(String... args) {
        if(debug) {
            String s = "OphProperties";
            for(String arg: args) {
                s = s + " " + arg;
            }
            System.out.println(s);
        }
    }

    public OphProperties addDefault(String key, String value) {
        defaults.put(key, value);
        return this;
    }
}
