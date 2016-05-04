package fi.vm.sade.javautils.cxf;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import fi.vm.sade.jetty.JettyJersey;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
//import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Tests for Caller-Id header insertion interceptor for cxf.
 * @author Jouni Stam
 *
 */
public class OphRequestHeadersCxfInterceptorTest {

    String unprotectedTargetUrl = "/mirror/headers";

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        JettyJersey.startServer("fi.vm.sade.javautils.cxf", null);
        //SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        JettyJersey.stopServer();
    }

    /**
     * Caller-Id:n tulisi tulla pyynnön headeriin.
     */
    @Test
    public void testCallerIdInsertion() throws IOException {
        OphRequestHeadersCxfInterceptor<Message> interceptor = createInterceptor();
        WebClient cxfClient = createClient(this.unprotectedTargetUrl, interceptor);
        String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
        assertContains(response, "clientSubSystemCode: TESTCLIENT", "CSRF: CSRF", "Cookie: CSRF=CSRF");
    }

    private static void assertContains(String from, String... args) {
        for(String arg: args) {
            Assert.assertTrue("String "+arg+" not found from: "+ from, from.contains(arg));
        }
    }

    private WebClient createClient(String url, OphRequestHeadersCxfInterceptor<Message> interceptor) {
        WebClient c = WebClient.create(getUrl(url)).accept(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON);
        // Add only as OUT interceptor
        WebClient.getConfig(c).getOutInterceptors().add(interceptor);
        return c;
    }
    
    private OphRequestHeadersCxfInterceptor<Message> createInterceptor() {
        OphRequestHeadersCxfInterceptor<Message> interceptor = new OphRequestHeadersCxfInterceptor<Message>();
        interceptor.setClientSubSystemCode("TESTCLIENT");
        return interceptor;
    }
    
    public static String getUrl(String url) {
        return JettyJersey.getUrl(url);
    }

}