package fi.vm.sade.authentication.cas;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Form;

import fi.vm.sade.jetty.JettyJersey;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.AssertionImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class CasFriendlyCxfInterceptorTest {

    static String unprotectedTargetUrl = "/casfriendly/unprotected";
    static String protectedTargetUrl = "/casfriendly/protected2/test";
    static String login = "whatever";
    static String password = "whatever";
    static String principalName = "whatever";
    static String wrongLogin = "deny";
    static String callerService = "CasFriendlyCxfInterceptorTest";
    static CasFriendlyCache cache = null;

    @BeforeClass
    public static void setUpClass() {
        cache = new CasFriendlyCache(3600, "CasFriendlyCxfInterceptorTest");
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        JettyJersey.startServer("fi.vm.sade.authentication.cas", null);
        SecurityContextHolder.clearContext();
        cache.clearAll();
    }

    @After
    public void tearDown() {
    }

    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - olemassa olevaa INVALID sessio, 
     *  - sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithLoginInvalidSessionRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, true, true, false);
            String targetServiceUrl = CasFriendlyHttpClient.resolveTargetServiceUrl(getUrl(protectedTargetUrl));
            interceptor.getCache().setSessionId("any", targetServiceUrl, login, "INVALID");
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 2, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 2);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - olemassa olevaa INVALID sessio, 
     *  - no sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithLoginInvalidSessionNoSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, false, true, false);
            String targetServiceUrl = CasFriendlyHttpClient.resolveTargetServiceUrl(getUrl(protectedTargetUrl));
            interceptor.getCache().setSessionId(callerService, targetServiceUrl, login, "INVALID");
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
            String sessionId = interceptor.getCache().getSessionId(callerService, targetServiceUrl, login);
            Assert.assertTrue("Session Id must not be INVALID any more, but is.", !"INVALID".equals(sessionId));
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithLoginNoSessionRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, true, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PALVELUTUNNUKSELLA POST
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithLoginNoSessionRequestPost() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, true, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            Form form = new Form();
            form.param("TESTNAME", "TESTVALUE");
            Response resp = cxfClient.form(form);
            String response = IOUtils.toString((InputStream) resp.getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - vaatii kirjautumista,
     *  - väärä tunnus/salasana
     */
    @Test
    public void testProtectedWithIncorrectLoginNoSessionRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    wrongLogin, password, null, true, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            cxfClient.get();
            Assert.assertTrue("Response status must be <> 200, got: " + cxfClient.getResponse().getStatus(), cxfClient.getResponse().getStatus() != 200);
            Assert.assertTrue("Session count should be 0, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 0);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - ei vaadi kirjautumista
     */
    @Test
    public void testUnprotectedWithSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, true, true, false);
            WebClient cxfClient = createClient(unprotectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * EI PALVELUTUNNUSTA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithoutLoginSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    null, null, null, true, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            cxfClient.get();
            Assert.assertTrue("Response status must be <> 200, got: " + cxfClient.getResponse().getStatus(), cxfClient.getResponse().getStatus() != 200);
            Assert.assertTrue("Session count should be 0, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 0);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * EI PALVELUTUNNUSTA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - sessionRequired,
     *  - ei vaadi kirjautumista
     */
    @Test
    public void testUnprotectedWithoutLoginSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    null, null, null, true, true, false);
            WebClient cxfClient = createClient(unprotectedTargetUrl, interceptor);
            cxfClient.get();
            Assert.assertTrue("Response status must be 200, got: " + cxfClient.getResponse().getStatus(), cxfClient.getResponse().getStatus() == 200);
            Assert.assertTrue("Session count should be 0, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 0);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * PALVELUTUNNUKSELLA
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - no sessionRequired,
     *  - ei vaadi kirjautumista
     */
    @Test
    public void testUnprotectedWithNoSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, false, true, false);
            WebClient cxfClient = createClient(unprotectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 0, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 0);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * PALVELUTUNNUKSELLA POST
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - no sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithNoSessionRequiredRequestPost() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, false, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            Form form = new Form();
            form.param("TESTNAME", "TESTVALUE");
            Response resp = cxfClient.form(form);
            String response = IOUtils.toString((InputStream) resp.getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * PALVELUTUNNUKSELLA GET
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - no sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithNoSessionRequiredRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    login, password, null, false, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            Response resp = cxfClient.get();
            String response = IOUtils.toString((InputStream) resp.getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PROXYLLA GET
     *  CASE:
     *  - ei olemassa olevaa sessiota, 
     *  - no sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithNoSessionRequiredProxyRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    null, null, CasFriendlyCasMockResource.fakeTgt, false, true, false);
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            Response resp = cxfClient.get();
            String response = IOUtils.toString((InputStream) resp.getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    
    /**
     * PROXYLLA GET
     *  CASE:
     *  - olemassa olevaa INVALID sessio, 
     *  - no sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedWithInvalidSessionNoSessionRequiredProxyRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    null, null, CasFriendlyCasMockResource.fakeTgt, false, true, false);
            String targetServiceUrl = CasFriendlyHttpClient.resolveTargetServiceUrl(getUrl(protectedTargetUrl));
            interceptor.getCache().setSessionId(callerService, targetServiceUrl, login, "INVALID");
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
            String sessionId = interceptor.getCache().getSessionId(callerService, targetServiceUrl, login);
            Assert.assertTrue("Session Id must not be INVALID any more, but is.", !"INVALID".equals(sessionId));
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    /**
     * PROXYLLA GET
     *  CASE:
     *  - ei sessiota, 
     *  - sessionRequired,
     *  - vaatii kirjautumista
     */
    @Test
    public void testProtectedSessionRequiredProxyRequestGet() {
        try {
            CasFriendlyCxfInterceptor<Message> interceptor = this.createInterceptor(
                    null, null, CasFriendlyCasMockResource.fakeTgt, true, true, false);
//            String targetServiceUrl = CasFriendlyHttpClient.resolveTargetServiceUrl(getUrl(protectedTargetUrl));
//            interceptor.getCache().setSessionId(callerService, targetServiceUrl, login, "INVALID");
            WebClient cxfClient = createClient(protectedTargetUrl, interceptor);
            String response = IOUtils.toString((InputStream) cxfClient.get().getEntity());
            Assert.assertTrue("Response should be: ok 1, but is: " + response, response.equals("ok 1"));
            Assert.assertTrue("Session count should be 1, but is: " + interceptor.getCache().getSize(), interceptor.getCache().getSize() == 1);
//            String sessionId = interceptor.getCache().getSessionId(callerService, targetServiceUrl, login);
//            Assert.assertTrue("Session Id must not be INVALID any more, but is.", !"INVALID".equals(sessionId));
        } catch(Exception ex) {
            ex.printStackTrace();
            Assert.assertTrue(false);
        }
    }
    
    private WebClient createClient(String url, CasFriendlyCxfInterceptor<Message> interceptor) {
        String testCaseId = interceptor.toString();
        WebClient c = WebClient.create(getUrl(url)).accept(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON).header("Testcase-Id", testCaseId);
        WebClient.getConfig(c).getOutInterceptors().add(interceptor);
        WebClient.getConfig(c).getInInterceptors().add(interceptor);
        return c;
    }
    
    private CasFriendlyCxfInterceptor<Message> createInterceptor(
            final String login, final String password, final String givenPgt, boolean sessionRequired, 
            boolean useSessionPerUser, boolean useBlockingConcurrent) {
        CasFriendlyCxfInterceptor<Message> interceptor = new CasFriendlyCxfInterceptor<Message>();
        if(givenPgt != null) {
            interceptor = new CasFriendlyCxfInterceptor<Message>() {
                @Override
                protected Authentication getAuthentication() {
                    AttributePrincipal principal = new AttributePrincipal() {
                        public String getProxyTicketFor(String service) {
                            return CasFriendlyCasMockResource.fakeSt;
                        }

                        @Override
                        public String getName() {
                            return principalName;
                        }

                        @Override
                        public Map getAttributes() {
                            return null;
                        }
                    };
                    Assertion assertion = new AssertionImpl(principal);
                    final Collection<? extends GrantedAuthority> authorities = new ArrayList<>();
                    final UserDetails userDetails = new User(CasFriendlyCxfInterceptorTest.login, CasFriendlyCxfInterceptorTest.password, Collections.<GrantedAuthority>emptyList());
                    CasAuthenticationToken token = new CasAuthenticationToken("test", principal, "test", authorities, userDetails, assertion);
                    return token;
                }
            };
        }
        
        interceptor.setCache(cache);
        interceptor.setAppClientUsername(login);
        interceptor.setAppClientPassword(password);
        interceptor.setSessionRequired(sessionRequired);
        interceptor.setCallerService(callerService);
        interceptor.setUseSessionPerUser(useSessionPerUser);
        interceptor.setUseBlockingConcurrent(useBlockingConcurrent);
        // Test with dev mode (cas is default)
//        interceptor.setAuthMode("dev");

        return interceptor;
    }
    
    public static String getUrl(String url) {
        return JettyJersey.getUrl(url);
    }

}