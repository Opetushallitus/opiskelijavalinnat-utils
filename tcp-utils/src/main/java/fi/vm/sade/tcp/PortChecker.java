package fi.vm.sade.tcp;

import java.io.IOException;
import java.net.Socket;

public final class PortChecker {
    private PortChecker() {};

    public static boolean isFreeLocalPort(int port) {
        try {
            Socket socket = new Socket("127.0.0.1", port);
            socket.close();
            return false;
        } catch(IOException e) {
            return true;
        }
    }

    public static int findFreeLocalPort() {
        int port = 1024 + (int) (60000 * Math.random());
        if (isFreeLocalPort(port)) {
            return port;
        } else {
            return findFreeLocalPort();
        }
    }
}
