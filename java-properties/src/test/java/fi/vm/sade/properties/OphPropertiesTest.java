package fi.vm.sade.properties;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class OphPropertiesTest {
    OphProperties ctx = null;
    Properties props = null;

    @Before
    public void before() {
        ctx = new OphProperties();
        ctx.ophProperties = props = new Properties();
    }

    @Test
    public void getUrl() {
        props.setProperty("service.baseUrl", "https://localhost/service");
        props.setProperty("service.action", "${service.baseUrl}/resources/action");

        assertEquals("https://localhost/service/resources/action", ctx.url("service.action"));
    }

    @Test
    public void require() {
        props.setProperty("a.b", "1");
        props.setProperty("b.b", "$1 $param");
        assertEquals("1", ctx.require("a.b"));
        assertEquals("A pow!", ctx.require("b.b", "A", new LinkedHashMap() {{
            put("param", "pow!");
        }}));
        try {
            ctx.require("c.c");
            throw new RuntimeException("Should not reach here");
        } catch (RuntimeException e) {
            assertEquals("\"c.c\" not defined.", e.getMessage());
        }
    }

    @Test
    public void getProperty() {
        props.setProperty("a.b", "1");
        assertEquals("1", ctx.getProperty("a.b"));
        assertEquals(null, ctx.getProperty("b.b"));
    }

    @Test
    public void resolveUrlAndThrowErrorOnUnknown() {
        props.setProperty("a.b", "1");
        assertEquals("1", ctx.url("a.b"));
        try {
            ctx.url("b.b");
            throw new RuntimeException("Should not reach here");
        } catch (RuntimeException e) {
            assertEquals("\"b.b\" not defined.", e.getMessage());
        }
    }

    @Test
    public void handleBaseUrl() {
        props.setProperty("a.a", "1");
        props.setProperty("b.b", "2");
        props.setProperty("c.c", "2");
        props.setProperty("a.baseUrl", "http://pow");
        props.setProperty("baseUrl", "http://bar");

        assertEquals("http://pow/1", ctx.url("a.a"));
        assertEquals("http://bar/2", ctx.url("b.b"));

        // ctx.overrides overrides baseUrl
        ctx.overrides.setProperty("baseUrl", "http://foo");
        assertEquals("http://pow/1", ctx.url("a.a"));
        assertEquals("http://foo/2", ctx.url("b.b"));

        // ctx.urls(baseUrl) overrides baseUrl and ctx.urls.defaults.override
        PropertyResolver ctx2 = ctx.urls("http://zap");
        assertEquals("http://pow/1", ctx2.url("a.a"));
        assertEquals("http://zap/2", ctx2.url("b.b"));

        // ctx.urls(null) should throw NPE
        try {
            ctx.urls(null);
            throw new RuntimeException("Should not reach here");
        } catch (NullPointerException e) {
            assertEquals("ophProperties.urls(null) not supported", e.getMessage());
        }
        // ctx.urls("1", null) should throw NPE
        try {
            ctx.urls("1", null);
            throw new RuntimeException("Should not reach here");
        } catch (NullPointerException e) {
            assertEquals("one parameter for ophProperties.urls() was null", e.getMessage());
        }
    }

    @Test
    public void parameterReplace() {
        props.setProperty("a.a", "/a/$1");
        props.setProperty("b.b", "/b/$param");
        assertEquals("/a/$1", ctx.url("a.a"));
        assertEquals("/a/1", ctx.url("a.a", 1));
        assertEquals("/b/$param", ctx.url("b.b"));
        assertEquals("/b/pow", ctx.url("b.b", new LinkedHashMap() {{
            put("param", "pow");
        }}));
        // extra named parameters go to queryString
        assertEquals("/b/pow?queryParameter=123&queryParameter2=123", ctx.url("b.b",
                        new LinkedHashMap() {{
                            put("param", "pow");
                            put("queryParameter", "123");
                            put("queryParameter2", "123");
                        }})
        );
        // extra named parameters go to queryString
        assertEquals("/b/pow?a=123&a=34&b=123&d=8&e=9&e=10", ctx.url("b.b",
                        new LinkedHashMap() {{
                            put("param", "pow");
                            put("a", Arrays.asList("123", "34"));
                            put("b", "123");
                            String[] singleString = {"8"};
                            String[] multipleString = {"9","10"};
                            put("d", singleString);
                            put("e", multipleString);
                        }})
        );
    }

    @Test
    public void parameterEncode() {
        props.setProperty("a.a", "/a/$1");
        props.setProperty("b.b", "/b/$param");
        assertEquals("/a/1%3A", ctx.url("a.a", "1:"));
        assertEquals("/b/pow%3A", ctx.url("b.b", new HashMap() {{
            put("param", "pow:");
        }}));
        assertEquals("/b/pow?query%20Parameter=1%3A23&query%20Parameter2=1%3A23", ctx.url("b.b", new LinkedHashMap() {{
            put("param", "pow");
            put("query Parameter", "1:23");
            put("query Parameter2", "1:23");
        }}));
        assertEquals("/b/pow?query%20Parameter=1%3A23&query%20Parameter=34&query%20Parameter2=1%3A23", ctx.url("b.b", new LinkedHashMap() {{
                    put("param", "pow");
                    put("query Parameter", Arrays.asList("1:23", "34"));
                    put("query Parameter2", "1:23");
                }}
        ));
        OphProperties.UrlResolver ctx2 = ctx.urls().noEncoding();
        assertEquals("/a/1:", ctx2.url("a.a", "1:"));
        assertEquals("/b/pow:", ctx2.url("b.b", new HashMap() {{
            put("param", "pow:");
        }}));
        assertEquals("/b/pow?query Parameter=1:23&query Parameter2=1:23", ctx2.url("b.b", new LinkedHashMap() {{
                    put("param", "pow");
                    put("query Parameter", "1:23");
                    put("query Parameter2", "1:23");
                }}
        ));
        assertEquals("/b/pow?query Parameter=1:23&query Parameter=34&query Parameter2=1:23", ctx2.url("b.b", new LinkedHashMap() {{
                    put("param", "pow");
                    put("query Parameter", Arrays.asList("1:23", "34"));
                    put("query Parameter2", "1:23");
                }}
        ));
    }

    @Test
    public void parameterAndUrlLookupOrder() {
        ctx.defaults.setProperty("a.a", "b");
        assertEquals("b", ctx.url("a.a"));

        props.setProperty("a.a", "c");
        assertEquals("c", ctx.url("a.a"));

        ctx.overrides.setProperty("a.a", "d");
        assertEquals("d", ctx.url("a.a"));

        PropertyResolver ctx2 = ctx.urls(new HashMap() {{
            put("a.a", "e");
        }});
        assertEquals("e", ctx2.url("a.a"));
    }

    @Test
    public void joinUrl() {
        assertEquals("a/b/c", UrlUtils.joinUrl("a","b","c"));
        assertEquals("a/b/c/d", UrlUtils.joinUrl("a/","b/","c/","/d"));
    }

    @Test
    public void parameterSubstitution() {
        assertEquals("https://POW/!", ctx.addDefault("host","POW").addDefault("url", "https://${host}/$1").require("url","!"));
    }

    @Test
    public void baseUrlOverride() throws URISyntaxException {
        new URI("hs.fi");
        ctx.addDefault("koodisto.url", "http://pow.fi/pow?123&a=1#/fpp");
        assertEquals("https://POW.FI/pow?123&a=1#/fpp", ctx.addDefault("baseUrl", "https://POW.FI").url("koodisto.url"));
        assertEquals("https://POW.FI/pow?123&a=1#/fpp", ctx.addDefault("baseUrl", "https://oph.fi").addDefault("koodisto.baseUrl", "https://POW.FI").url("koodisto.url"));
    }

    @Test
    public void testFrontToJson() {
        ctx.ophProperties.put("b","1");
        ctx.frontProperties.put("a","a!.\n\"");
        ctx.frontProperties.put("bRec","${b}");
        assertEquals("{\n" +
                "\"a\": \"a!.\\\\n\\\"\",\n" +
                "\"bRec\": \"1\"\n" +
                "}",ctx.frontPropertiesToJson());
    }

    @Test
    public void resolveUrlShouldNotThrowExceptionIfNoService() {
        ctx.ophProperties.put("a","a/$1");
        assertEquals("a/POW!", ctx.url("a", "POW!"));
    }

    @Test
    public void resolveFor() {
        ctx.ophProperties.put("a.a","a/$1");
        assertEquals("a/POW!", ctx.resolveFor("a.a").url("POW!"));
        try {
            ctx.resolveFor("b.b");
            throw new RuntimeException("Should not reach here");
        } catch (RuntimeException e) {
            assertEquals("\"b.b\" not defined.", e.getMessage());
        }
    }

    @Test
    public void resolveSpringValueDefault() {
        ctx.ophProperties.put("a","a/${1:b}");
        assertEquals("a/b", ctx.url("a", "POW!"));
    }
}
