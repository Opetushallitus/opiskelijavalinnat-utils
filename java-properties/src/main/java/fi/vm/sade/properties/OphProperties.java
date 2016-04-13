package fi.vm.sade.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static fi.vm.sade.properties.UrlUtils.joinUrl;

/**
 * Reads property values from classpath files, files in filesystem paths and system properties (-D).
 * Supports parameter replace ($1 & $name), recursive reference for values: ${key:optionalDefaultValue} and url generation from service.url (looks up service.baseUrl)
 * Collects configuration to be used at front (application code can configure separately which files are loaded and which system properties prefixes are used for front).
 */
public class OphProperties implements PropertyResolver {
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
                    getPropertiesWithPrefix(defaults, "url.", "front."),
                    getPropertiesWithPrefix(ophProperties, "url.", "front."),
                    getPropertiesWithPrefix(overrides, "url.", "front."),
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

    @Override
    public String require(String key, Object... params) {
        return requireProperty(key, params, replacer, true, overrides, ophProperties, defaults);
    }

    @Override
    public String getProperty(String key, Object... params) {
        return getOrElse(key, null, params);
    }

    /**
     * Return defaultValue if value not defined. Format returned string with params
     * Resolve order: overrides, ophProperties, defaults
     * @param key
     * @param defaultValue
     * @param params
     * @return
     */
    @Override
    public String getOrElse(String key, String defaultValue, Object... params) {
        return resolveProperty(key, defaultValue, params, replacer, true, overrides, ophProperties, defaults);
    }

    private String resolveProperty(String key, String defaultValue, Object[] params, ParamReplacer replacer, boolean printDebug, Properties... properties) {
        for(Properties props: properties) {
            if(props.containsKey(key)) {
                return replaceParams((String) props.get(key), params, replacer, properties, key, printDebug);
            }
        }
        return replaceParams(defaultValue, params, replacer, properties, key, printDebug);
    }

    private String replaceParams(String value, Object[] params, ParamReplacer replacer, Properties[] properties, String key, boolean printDebug) {
        if (value != null) {
            value = replacer.replaceParams(value, convertParams(params));
            value = resolveRecursiveReferences(value, properties);
        }
        if(printDebug) {
            debug(key, "->", value);
        }
        return value;
    }

    private String resolveRecursiveReferences(String value, Properties[] properties) {
        int start,end;
        while((start=value.indexOf("${"))!=-1) {
            end = value.indexOf("}", start+2);
            if(end == -1) {
                throw new RuntimeException("Value contains open key reference: " + value);
            }
            String substring = value.substring(start + 2, end);
            String args[] = substring.split(":");
            String key=args[0],defaultValue=null, subValue;
            if(args.length == 2) {
                subValue = resolveProperty(key, defaultValue, new Object[0], replacer, false, properties);
            } else {
                subValue = requireProperty(key, new Object[0], replacer, false, properties);
            }
            value = value.substring(0, start) + subValue + value.substring(end+1);
        }
        return value;
    }

    private String requireProperty(String key, Object[] params, ParamReplacer replacer, boolean printDebug, Properties... properties) {
        for(Properties props: properties) {
            if(props.containsKey(key)) {
                String value = (String) props.get(key);
                return replaceParams(value, params, replacer, properties, key, printDebug);
            }
        }
        throw new RuntimeException("\"" + key + "\" not defined.");
    }

    /**
     * Resolves url for the key.
     * @param key
     * @param params
     * @return
     */
    @Override
    public String url(String key, Object... params) {
        return new UrlResolver().url(key, params);
    }

    /**
     * Return a new PropertyResolver for urls. Parameters override properties in parent OphProperties. String parameter is set to "baseUrl"
     * Resolve order: urlsConfig, overrides, ophProperties, defaults
     * @param args
     * @return
     */
    public UrlResolver urls(Object... args) {
        if(args == null) {
            throw new NullPointerException("ophProperties.urls(null) not supported");
        }
        Properties urlsConfig = new Properties();
        for (Object o : args) {
            if(o == null) {
                throw new NullPointerException("one parameter for ophProperties.urls() was null");
            } else if (o instanceof Map) {
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

    public String frontPropertiesToJson() {
        return mapToJson(frontProperties);
    }

    public class UrlResolver extends ParamReplacer implements PropertyResolver {
        private final Properties urlsConfig = new Properties();
        private boolean encode = true;

        public UrlResolver(Properties urlsConfig) {
            this();
            merge(this.urlsConfig, urlsConfig);
        }

        public UrlResolver() {
            ensureLoad();
        }

        public UrlResolver baseUrl(String baseUrl) {
            urlsConfig.put("baseUrl", baseUrl);
            return this;
        }

        public UrlResolver noEncoding() {
            encode = false;
            return this;
        }

        @Override
        public String require(String key, Object... params) {
            return requireProperty(key, params, this, true, urlsConfig, overrides, ophProperties, defaults);
        }

        @Override
        public String getProperty(String key, Object... params) {
            return getOrElse(key, null, params);
        }

        @Override
        public String getOrElse(String key, String defaultValue, Object... params) {
            return resolveProperty(key, defaultValue, params, this, true, urlsConfig, overrides, ophProperties, defaults);
        }

        @Override
        public String url(String key, Object... params) {
            String url = require(key, params);
            Object baseUrl = getProperty(parseService(key) + ".baseUrl");
            if (baseUrl == null) {
                baseUrl = getProperty("baseUrl");
            }
            if (baseUrl != null) {
                String strippedUrl = stripBaseUrl(url);
                url = joinUrl(baseUrl.toString(), strippedUrl);
            }
            debug("url:", key, "->", url);
            return url;
        }

        private String stripBaseUrl(String url) {
            URI uri;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            String stripped = "";
            if(uri.getRawPath() != null) {
                stripped += uri.getRawPath();
            }
            if(uri.getRawQuery() != null) {
                stripped += "?" + uri.getRawQuery();
            }
            if(uri.getRawFragment() != null) {
                stripped += "#" + uri.getRawFragment();
            }
            return stripped;
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

    // Simplified JSON map generator. Escapes newlines and " chars
    public static String mapToJson(Map map) {
        StringBuilder buf = new StringBuilder("{\n");
        boolean first = true;
        for(Object key: map.keySet()) {
            if(first) {
                first = false;
            } else {
                buf.append(",\n");
            }
            buf.append('"').append(escapeForJson(key.toString())).append("\": ");
            Object value = map.get(key);
            if(value == null) {
                buf.append("null");
            } else {
                buf.append('"').append(escapeForJson(value.toString())).append('"');
            }
        }
        buf.append("\n}");
        return buf.toString();
    }

    public static String escapeForJson(String s) {
        return s.replace("\n", "\\\\n")
                .replace("\"", "\\\"");
    }
}
