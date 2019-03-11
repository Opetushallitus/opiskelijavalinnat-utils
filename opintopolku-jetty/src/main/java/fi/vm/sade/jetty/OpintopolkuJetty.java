package fi.vm.sade.jetty;

import ch.qos.logback.access.jetty.RequestLogImpl;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class OpintopolkuJetty {
    public static final int SERVICE_PORT_IN_ECS_CONFIGURATION = 8080;
    protected static final Logger LOG = LoggerFactory.getLogger(OpintopolkuJetty.class);

    public void start(String contextPath) {
        start(contextPath, SERVICE_PORT_IN_ECS_CONFIGURATION, 5, 10, Duration.ofMinutes(1));
    }

    public void start(String contextPath, int port, int minThreads, int maxThreads, Duration idleThreadTimeout) {
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setBaseResource(Resource.newClassPathResource("/webapp"));
        start(webAppContext, createServer(port, minThreads, maxThreads, idleThreadTimeout), contextPath);
    }

    private Server createServer(int port, int minThreads, int maxThreads, Duration idleThreadTimeout) {
        int idleThreadTimeoutMs = (int) idleThreadTimeout.toMillis();
        ThreadPool threadPool = createThreadpool(minThreads, maxThreads, idleThreadTimeoutMs);
        Server server = new Server(threadPool);
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(port);
        server.setConnectors(new Connector[]{ serverConnector });
        return server;
    }

    protected ThreadPool createThreadpool(int minThreads, int maxThreads, int idleThreadTimeoutMs) {
        return new QueuedThreadPool(maxThreads, minThreads, idleThreadTimeoutMs);
    }

    public void start(WebAppContext webAppContext, Server server, String contextPath) {
        try {
            if (server.isStopped()) {
                webAppContext.setContextPath(contextPath);
                LOG.info(String.format("Starting Jetty %s at port %d for context %s to path %s",
                    Jetty.VERSION, ((ServerConnector) server.getConnectors()[0]).getPort(), webAppContext.getWar(), webAppContext.getContextPath()));
                webAppContext.setParentLoaderPriority(true);
                server.setHandler(webAppContext);
                server.setStopAtShutdown(true);
                server.setRequestLog(createAccessLogConfiguration());
                server.start();
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected RequestLog createAccessLogConfiguration() {
        RequestLogImpl requestLog = new RequestLogImpl();
        String logbackAccess = System.getProperty("logback.access");
        if (logbackAccess != null) {
            requestLog.setFileName(logbackAccess);
        } else {
            LOG.warn("Jetty access log is printed to console, use -Dlogback.access=path/to/logback-access.xml to set configuration file");
            requestLog.setResource("/logback-access-to-stdout.xml");
        }
        requestLog.start();
        return requestLog;
    }
}
