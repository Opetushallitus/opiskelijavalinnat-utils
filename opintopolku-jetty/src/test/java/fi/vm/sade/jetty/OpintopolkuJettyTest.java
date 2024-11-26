package fi.vm.sade.jetty;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.Random;

public class OpintopolkuJettyTest {
    private final OpintopolkuJetty jetty = new OpintopolkuJetty();

    @Test
    public void opintopolkuJettyServesContentFromGivenClasspathLocation() throws Exception {
        int port = findFreeLocalPort();
        String webappPath = "/testing";
        jetty.start(webappPath, port, 1, 5, Duration.ofSeconds(10), Duration.ofSeconds(4000));

        URL url = new URL(String.format("http://localhost:%d%s/hello.html", port, webappPath));

        String responseContent;

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try (AutoCloseable conWrapper = connection::disconnect) {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            responseContent = IOUtils.toString(connection.getInputStream(), "UTF-8");
        }

        assertThat(responseContent, containsString("Hello, world according to OpintopolkuJetty!"));
    }

    public final static boolean isFreeLocalPort(int port) {
        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", port);
            socket.close();
        } catch (IOException e) {
            return true;
        }
        return false;
    }

    public final static int findFreeLocalPort() {
        int port = new Random().nextInt(60000) + 1000;
        if (isFreeLocalPort(port)) {
            return port;
        } else {
            return findFreeLocalPort();
        }
    }
}
