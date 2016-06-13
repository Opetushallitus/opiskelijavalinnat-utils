package fi.vm.sade.integrationtest.util;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class PortChecker {

    public static void assertPortsAreFree(Integer... ports) {
        for(int port: ports) {
            if(!isFreeLocalPort(port)) {
                throw new RuntimeException("TCP port is not free for localhost:" +port);
            }
        }
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
