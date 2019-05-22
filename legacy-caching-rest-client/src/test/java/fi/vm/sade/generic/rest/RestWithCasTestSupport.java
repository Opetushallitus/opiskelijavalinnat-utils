package fi.vm.sade.generic.rest;

import fi.vm.sade.jetty.JettyJersey;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.security.core.context.SecurityContextHolder;

public class RestWithCasTestSupport {

    CachingRestClient client;

    @BeforeClass
    public static void start() throws Exception {
        JettyJersey.startServer("fi.vm.sade.generic.rest", null);
    }

    @Before
    public void init() {
        TestParams.instance = new TestParams();
        HttpTestResource.counter = 1;
        HttpTestResource.someResource = "original value";
        SecurityContextHolder.clearContext();
//        DefaultTicketCachePolicy.ticketThreadLocal.remove();
        client = new CachingRestClient("RestWithCasTestSupport");
        client.setWebCasUrl("N/A");
    }

    @AfterClass
    public static void stop() throws Exception {
        JettyJersey.stopServer();
    }

    protected String getUrl(String url) {
        return JettyJersey.getUrl(url);
    }

    public void assertCas(int redirects, int tgtsCreated, int ticketsCreated, int requestAuthenticationCalled, int ticketsValidatedAgainstCasSuccessfully) {
        Assert.assertEquals("error in redirects count", redirects, TestParams.instance.authRedirects);
        Assert.assertEquals("error in tgtsCreated count", tgtsCreated, TestParams.instance.authTgtCount);
        Assert.assertEquals("error in ticketsCreated count", ticketsCreated, TestParams.instance.authTicketCount);
        Assert.assertEquals("error in requestAuthenticationCalled count", requestAuthenticationCalled, TestParams.instance.isRequestAuthenticatedCount);
        Assert.assertEquals("error in ticketsValidatedAgainstCasSuccessfully count", ticketsValidatedAgainstCasSuccessfully, TestParams.instance.authTicketValidatedSuccessfullyCount);
    }

}
