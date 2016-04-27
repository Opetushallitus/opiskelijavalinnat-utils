package fi.vm.sade.properties;

public interface ValueResolver {
    String require(Object... params);
    String getProperty(Object... params);
    String getOrElse(String defaultValue, Object... params);
    String url(Object... params);
}