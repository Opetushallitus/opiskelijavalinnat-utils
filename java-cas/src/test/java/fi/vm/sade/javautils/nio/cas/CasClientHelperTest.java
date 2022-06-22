package fi.vm.sade.javautils.nio.cas;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.hamcrest.core.IsInstanceOf;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class CasClientHelperTest {
    private MockWebServer mockWebServer;
    private CasClient casClient;
    private CasClientHelper casClientHelper;
    private static final String CSRF_VALUE = "CSRF";
    private static final String COOKIENAME = "JSESSIONID";
    private static final String VALID_TICKET = "it-ankan-tiketti";
    private static final String VALID_NEW_TICKET = "it-ankan-tiketti-2";

    @Before
    public void init() {
        this.mockWebServer = new MockWebServer();
        this.casClient = CasClientBuilder.build(new CasConfig.CasConfigBuilder("it-ankka",
                "neverstopthemadness",
                mockWebServer.url("/cas").toString(),
                mockWebServer.url("/cas/") + "test-service",
                "CSRF",
                "Caller-Id",
                "/j_spring_cas_security_check").setJsessionName(COOKIENAME).build());
        this.casClientHelper = new CasClientHelper(casClient);
    }

    @After
    public void shutDown() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


     
    @Test
    public void shouldGet() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Location", mockWebServer.url("/") + "cas/tickets")
                .setResponseCode(201));
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_TICKET)
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Set-Cookie: " + String.format(COOKIENAME + "=%s; Path=/test-service/", "123456789"))
                .addHeader("Set-Cookie: " + String.format("TEST-COOKIE=%s; Path=/test-service/", "WHUTEVAMAN"))
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("{ test: \"testi\"}")
                .setResponseCode(200));

        TestPojo testPojo = this.casClientHelper.doGetSync(this.mockWebServer.url("/test").toString(), TestPojo.class);

        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest actualRequest = mockWebServer.takeRequest();
        assertEquals("/test", actualRequest.getPath());
        assertEquals(true, actualRequest.getHeader("cookie").contains("JSESSIONID=123456789"));
        assertEquals("testi", testPojo.test);
    }

    @Test
    public void shouldPost() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Location", mockWebServer.url("/") + "cas/tickets")
                .setResponseCode(201));
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_TICKET)
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Set-Cookie: " + String.format(COOKIENAME + "=%s; Path=/test-service/", "123456789"))
                .addHeader("Set-Cookie: " + String.format("TEST-COOKIE=%s; Path=/test-service/", "WHUTEVAMAN"))
                .setResponseCode(200));
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setResponseCode(204));

        TestPojo testPojo = new TestPojo();
        testPojo.test = "testi";
        Response response = this.casClientHelper.doPostSync(this.mockWebServer.url("/test").toString(), testPojo);

        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        mockWebServer.takeRequest();
        RecordedRequest actualRequest = mockWebServer.takeRequest();
        assertEquals("/test", actualRequest.getPath());
        assertEquals(true, actualRequest.getHeader("cookie").contains("JSESSIONID=123456789"));
        assertEquals(204, response.getStatusCode());
        assertEquals("{\"test\":\"testi\"}", actualRequest.getBody().readString(Charset.defaultCharset()));
    }

    public class TestPojo {
        public String test;
        @Override
        public String toString() {
            return "TestPojo{" +
                    "test='" + test + '\'' +
                    '}';
        }
    }
}

