package fi.vm.sade.integrationtest.tomcat;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.SpringProfile;
import org.apache.catalina.Server;

/**
 * Ensures that there is only one instance of webserver running.
 */
public class SharedTomcat {
    private static EmbeddedTomcat shared;

    public final static int port = PortChecker.findFreeLocalPort();

    public static synchronized EmbeddedTomcat start(String moduleRoot, String contextPath) {
        create(moduleRoot, contextPath).start();
        return shared;
    }

    public static EmbeddedTomcat create(String moduleRoot, String contextPath) {
        SpringProfile.setProfile("it");
        return new EmbeddedTomcat(port, moduleRoot, contextPath){
            @Override
            public Server start() {
                synchronized (SharedTomcat.class) {
                    if (shared != null && !shared.appConfigIsEqual(this)) {
                        throw new IllegalStateException("Shared Tomcat already running with different contextPath or moduleRoot. Existing instance=" + shared);
                    }
                    if (shared == null) {
                        shared = this;
                        super.start();
                    }
                    return shared.getServer();
                }
            }
        };
    }
}
