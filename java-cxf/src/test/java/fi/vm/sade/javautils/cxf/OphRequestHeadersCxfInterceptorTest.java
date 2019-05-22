package fi.vm.sade.javautils.cxf;

import fi.vm.sade.jetty.JettyJersey;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
public class OphRequestHeadersCxfInterceptorTest {
    private static final String CALLER_ID = "1.2.246.562.10.00000000001.java-cxf.TESTCLIENT";
    private String unprotectedTargetUrl = "/mirror/headers";
    private final OphRequestHeadersCxfInterceptor<Message> interceptor = createInterceptor();

    @Before
    public void setUp() throws Exception {
        JettyJersey.startServer("fi.vm.sade.javautils.cxf", null);
    }

    @After
    public void tearDown() {
        JettyJersey.stopServer();
    }

    @Test
    public void testCallerIdInsertion() throws IOException {
        String response = IOUtils.toString((InputStream) createClient(this.unprotectedTargetUrl, interceptor).get().getEntity());
        assertContains(response, "Caller-Id: " + CALLER_ID, "CSRF: CSRF", "Cookie: CSRF=CSRF");
    }

    @Test
    public void testMultipleCookieValues() throws IOException {
        WebClient client = createClient(this.unprotectedTargetUrl, interceptor)
            .header("Cookie", "X-Foo=baar; X-Wing=Destroyer");
        String response = IOUtils.toString((InputStream) client.get().getEntity());
        assertContains(response, "Caller-Id: " + CALLER_ID, "CSRF: CSRF", "Cookie: X-Foo=baar; X-Wing=Destroyer; CSRF=CSRF");
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
        return new OphRequestHeadersCxfInterceptor<Message>(CALLER_ID);
    }
    
    public static String getUrl(String url) {
        return JettyJersey.getUrl(url);
    }
}
