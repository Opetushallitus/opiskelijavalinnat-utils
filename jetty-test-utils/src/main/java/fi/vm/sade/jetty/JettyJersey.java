package fi.vm.sade.jetty;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import fi.vm.sade.tcp.PortChecker;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Helper class to start embedded jetty + jersey for tests.
 *
 * @author Antti Salonen
 */
public class JettyJersey {
    static Server server;
    static int port;

    public static void startServer(String packageContainingJerseyRestResources, String jerseyFilterClasses) throws Exception {

        port = PortChecker.findFreeLocalPort();

        System.setProperty("cas_key", getUrl("testing"));
        System.setProperty("cas_service", getUrl("/httptest"));
        System.setProperty("web.url.cas", getUrl("/mock_cas/cas"));

        server = new Server(port);
        Context root = new Context(server, "/", Context.SESSIONS);
        ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        servletHolder.setInitOrder(1); // have to be set so that jersey will load on startup (otherwise might cause problems in cache timeout tests..)
        servletHolder.setInitParameter("com.sun.jersey.config.property.resourceConfigClass", "com.sun.jersey.api.core.PackagesResourceConfig");
        servletHolder.setInitParameter("com.sun.jersey.config.property.packages", packageContainingJerseyRestResources);
//        servletHolder.setInitParameter("com.sun.jersey.config.feature.Debug", "true");
//        servletHolder.setInitParameter("com.sun.jersey.config.feature.Trace", "true");
//        servletHolder.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
        servletHolder.setInitParameter("com.sun.jersey.spi.container.ContainerResponseFilters", /*"com.sun.jersey.api.container.filter.LoggingFilter,"*/""+(jerseyFilterClasses != null ? jerseyFilterClasses : ""));
        root.addServlet(servletHolder, "/*");
        server.start();
        System.out.println("jetty started at port "+port);
    }

    public static void stopServer() throws Exception {
        server.stop();
    }

    public static int getPort() {
        return port;
    }

    public static String getUrl(String url) {
        return "http://localhost:"+ getPort()+url;
    }
}
