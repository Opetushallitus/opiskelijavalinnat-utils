package fi.vm.sade.properties;

public class ValueResolverImpl implements ValueResolver {
    private final PropertyResolver resolver;
    private final String key;

    public ValueResolverImpl(PropertyResolver resolver, String key) {
        this.resolver = resolver;
        this.key = key;
    }

    @Override
    public String require(Object... params) {
        return resolver.require(key, params);
    }

    @Override
    public String getProperty(Object... params) {
        return resolver.getProperty(key, params);
    }

    @Override
    public String getOrElse(String defaultValue, Object... params) {
        return resolver.getOrElse(key, defaultValue, params);
    }

    @Override
    public String url(Object... params) {
        return resolver.url(key, params);
    }
}
