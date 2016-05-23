package fi.vm.sade.javautils.httpclient;

import fi.vm.sade.properties.OphProperties;
import org.junit.*;

import java.io.IOException;

public class OphHttpClientTest {

    private OphHttpClient client;

    @Before
    public void setUp() throws Exception {
        OphProperties properties = new OphProperties().
                addDefault("local.mirror", "/mirror/headers");
//                addDefault("baseUrl", "http://localhost:" + JettyJersey.getPort());
        client = ApacheOphHttpClient.createDefaultOphHttpClient("TESTCLIENT", properties, 1000, 1000);
    }

    @After
    public void tearDown() {
    }

    @Test
    @Ignore
    public void testGetHeaders() throws IOException {
        client.get("local.mirror").execute(new OphHttpResponseHandler<Void>() {
            @Override
            public Void handleResponse(OphHttpResponse response) {
                assertContains(response.asText(), "clientSubSystemCode: TESTCLIENT", "CSRF: CSRF", "Cookie: CSRF=CSRF");
                return null;
            }
        });
    }

    private static void assertContains(String from, String... args) {
        for(String arg: args) {
            Assert.assertTrue("String "+arg+" not found from: "+ from, from.contains(arg));
        }
    }
}