package fi.vm.sade.generic.rest;

import org.junit.Before;
import org.junit.Test;

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
    public void require() {
        ctx.ophProperties.setProperty("a.b", "1");
        ctx.ophProperties.setProperty("b.b", "$1 $param");
        assertEquals("1", ctx.require("a.b"));
        assertEquals("A pow!", ctx.require("b.b", "A", new LinkedHashMap() {{
            put("param", "pow!");
        }}));
        try {
            ctx.require("c.c");
            throw new RuntimeException("Should not reach here");
        } catch (RuntimeException e) {
            assertEquals("'c.c' not defined.", e.getMessage());
        }
    }

    @Test
    public void getProperty() {
        ctx.ophProperties.setProperty("a.b", "1");
        assertEquals("1", ctx.getProperty("a.b"));
        assertEquals(null, ctx.getProperty("b.b"));
    }

    @Test
    public void resolveUrlAndThrowErrorOnUnknown() {
        ctx.ophProperties.setProperty("a.b", "1");
        assertEquals("1", ctx.url("a.b"));
        try {
            ctx.url("b.b");
            throw new RuntimeException("Should not reach here");
        } catch (RuntimeException e) {
            assertEquals("'b.b' not defined.", e.getMessage());
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

        // ctx.defaultOverrides overrides baseUrl
        ctx.defaultOverrides.setProperty("baseUrl", "http://foo");
        assertEquals("http://pow/1", ctx.url("a.a"));
        assertEquals("http://foo/2", ctx.url("b.b"));

        // ctx.urls(baseUrl) overrides baseUrl and ctx.urls.defaults.override
        OphProperties.UrlResolver ctx2 = ctx.urls("http://zap");
        assertEquals("http://pow/1", ctx2.url("a.a"));
        assertEquals("http://zap/2", ctx2.url("b.b"));
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
    }

    @Test
    public void parameterAndUrlLookupOrder() {
        ctx.defaults.setProperty("a.a", "b");
        assertEquals("b", ctx.url("a.a"));

        props.setProperty("a.a", "c");
        assertEquals("c", ctx.url("a.a"));

        ctx.defaultOverrides.setProperty("a.a", "d");
        assertEquals("d", ctx.url("a.a"));

        OphProperties.UrlResolver ctx2 = ctx.urls(new HashMap() {{
            put("a.a", "e");
        }});
        assertEquals("e", ctx2.url("a.a"));
    }
}
