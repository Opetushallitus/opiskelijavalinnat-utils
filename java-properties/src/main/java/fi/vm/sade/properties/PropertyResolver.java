package fi.vm.sade.properties;

public interface PropertyResolver {
    String require(String key, Object... params);
    String getProperty(String key, Object... params);
    String getOrElse(String key, String defaultValue, Object... params);
    String url(String key, Object... params);
}