package fi.vm.sade.authentication.cas;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import fi.vm.sade.jetty.JettyJersey;
import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

public class CasFriendlyHttpClientTest {

    String unprotectedTargetUrl = "/casfriendly/unprotected";
    String protectedTargetUrl = "/casfriendly/protected2/test";
    String login = "whatever";
    String password = "whatever";

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        JettyJersey.startServer("fi.vm.sade.authentication.cas", null);
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testUnauthenticatedNotProtectedRequestGet() {
        HttpGet request = null;
        try {
            CasFriendlyHttpClient client = new CasFriendlyHttpClient();
            request = new HttpGet(getUrl(unprotectedTargetUrl));
            request.setHeader("Testcase-Id", client.toString());
            HttpResponse response = client.execute(request);
            if(response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is,bos);
                bos.flush();
                bos.close();
                Assert.assertTrue("Expecting response content to start with 'ok 1', but got: " + bos.toString(), bos.toString().startsWith("ok 1"));
            }
            Assert.assertTrue("Response not 200 (as expected), but " + response.getStatusLine().getStatusCode(), response.getStatusLine().getStatusCode() == 200);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        } finally {
            request.releaseConnection();
        }
    }

    @Test
    public void testUnauthenticatedProtectedRequestGet() {
        HttpGet request = null;
        try {
            CasFriendlyHttpClient client = new CasFriendlyHttpClient();
            request = new HttpGet(getUrl(protectedTargetUrl));
            request.setHeader("Testcase-Id", client.toString());
            HttpContext context = client.createHttpContext(null, null, null);
            HttpResponse response = client.execute(request, context);
            if(response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is,bos);
                bos.flush();
                bos.close();
                Assert.assertTrue(bos.size() > 0);
            }
            Assert.assertTrue("Response not error (as expected), but " + response.getStatusLine().getStatusCode(), response.getStatusLine().getStatusCode() != 200);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        } finally {
            request.releaseConnection();
        }
    }

    @Test
    public void testAuthenticatedProtectedRequestGet() {
        HttpGet request = null;
        try {
            CasFriendlyCache cache = new CasFriendlyCache(1, "testcache");
            CasFriendlyHttpClient client = new CasFriendlyHttpClient();
            HttpContext context = client.createHttpContext(login, password, cache);
            request = new HttpGet(getUrl(protectedTargetUrl));
            request.setHeader("Testcase-Id", client.toString());
            HttpResponse response = client.execute(request, context);
            if(response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is,bos);
                bos.flush();
                bos.close();
                Assert.assertTrue("Expecting response content to start with 'ok 1', but got: " + bos.toString(), bos.toString().startsWith("ok 1"));
            } else {
                Assert.assertTrue("No response body.", false);
            }
            Assert.assertTrue("Response not 200 (OK), but " + response.getStatusLine().getStatusCode(), response.getStatusLine().getStatusCode() == 200);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        } finally {
            request.releaseConnection();
        }
    }

    @Test
    public void testAuthenticatedSessionRequestGet() {
        HttpGet request = null;
        try {
            CasFriendlyCache cache = new CasFriendlyCache(1, "testcache");
            CasFriendlyHttpClient client = new CasFriendlyHttpClient();
            HttpContext context = client.createHttpContext(login, password, cache);
            request = new HttpGet(getUrl(protectedTargetUrl));
            request.setHeader("Testcase-Id", client.toString());
            HttpResponse response = client.execute(request, context);
            if(response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is,bos);
                bos.flush();
                bos.close();
                Assert.assertTrue("Expecting response content to start with 'ok 1', but got: " + bos.toString(), bos.toString().startsWith("ok 1"));
            } else {
                Assert.assertTrue("No response body.", false);
            }
            Assert.assertTrue("Response not 200 (OK), but " + response.getStatusLine().getStatusCode(), response.getStatusLine().getStatusCode() == 200);
            
            // Reconstruct client
            client = new CasFriendlyHttpClient();
            request = new HttpGet(getUrl(protectedTargetUrl));
            request.setHeader("Testcase-Id", client.toString());
            // Use existing context
            response = client.execute(request, context);
            if(response.getEntity() != null) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(is,bos);
                bos.flush();
                bos.close();
                Assert.assertTrue("Expecting response content to start with 'ok 1', but got: " + bos.toString(), bos.toString().startsWith("ok 1"));
            } else {
                Assert.assertTrue("No response body.", false);
            }
            Assert.assertTrue("Response not 200 (OK), but " + response.getStatusLine().getStatusCode(), response.getStatusLine().getStatusCode() == 200);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        } finally {
            request.releaseConnection();
        }
    }

    protected String getUrl(String url) {
        return JettyJersey.getUrl(url);
    }

}