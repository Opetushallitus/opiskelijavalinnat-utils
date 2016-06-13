package fi.vm.sade.integrationtest.tomcat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.builder.ToStringBuilder;

import fi.vm.sade.integrationtest.util.SpringProfile;
import fi.vm.sade.integrationtest.util.PortChecker;

public class EmbeddedTomcat {
    public final int port;
    public final int ajpPort;
    public final List<WebAppConfig> apps = new ArrayList<>();
    private Tomcat tomcat;

    public EmbeddedTomcat(final int port, String moduleRoot, String contextPath) {
        this.port = port != 0 ? port : PortChecker.findFreeLocalPort();
        this.ajpPort = PortChecker.findFreeLocalPort();
        addWebApp(moduleRoot, contextPath);
    }

    public EmbeddedTomcat(final int port, final int ajpPort, String moduleRoot, String contextPath) {
        this.port = port != 0 ? port : PortChecker.findFreeLocalPort();
        this.ajpPort = ajpPort != 0 ? ajpPort : PortChecker.findFreeLocalPort();
        addWebApp(moduleRoot, contextPath);
    }

    public EmbeddedTomcat addWebApp(String moduleRoot, String contextPath) {
        apps.add(new WebAppConfig(moduleRoot, contextPath));
        return this;
    }

    public Server start() {
        if (tomcat == null) {
            try {
                this.tomcat = new Tomcat() {
                    @Override
                    public void start() throws LifecycleException {
                        super.start();
                        Runtime.getRuntime().addShutdownHook(new Thread("Tomcat work directory delete hook") {
                            @Override
                            public void run() {
                                try {
                                    org.apache.commons.io.FileUtils.deleteDirectory(new File(basedir));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                };
                PortChecker.assertPortsAreFree(this.port, this.ajpPort);
                tomcat.setPort(this.port);
                for(WebAppConfig app : apps) {
                    app.webappDirLocation = app.moduleRoot + "/src/main/webapp/";
                    app.ctx = tomcat.addWebapp(app.contextPath, app.webappDirLocation);
                    String webXml = getWebXml(app.moduleRoot);
                    System.out.println("EmbeddedTomcat: starting " + app.contextPath + " from " + app.webappDirLocation + " with " + webXml);
                    setInitialContext(app.moduleRoot, app.ctx);
                    app.ctx.getServletContext().setAttribute(Globals.ALT_DD_ATTR, webXml);
                }
                final Connector ajpConnector = new Connector("AJP/1.3");
                ajpConnector.setScheme("ajp");
                ajpConnector.setPort(this.ajpPort);
                tomcat.getService().addConnector(ajpConnector);
                tomcat.start();
                for(WebAppConfig app : apps) {
                    if(!app.ctx.getState().isAvailable()) {
                        tomcat.stop();
                        tomcat.getServer().await();
                        throw new RuntimeException("Tomcat context failed to start for " + app.contextPath + " at " + app.webappDirLocation);
                    } else {
                        System.out.println("EmbeddedTomcat: started " + app.contextPath + " from " + app.webappDirLocation);
                    }
                }
            } catch (LifecycleException e) {
                throw new RuntimeException(e);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
        return tomcat.getServer();
    }

    public Server getServer() {
        return tomcat.getServer();
    }

    public boolean appConfigIsEqual(EmbeddedTomcat currentlyRunning) {
        List<WebAppConfig> currentlyRunningApps = currentlyRunning.apps;
        if(currentlyRunningApps.size() != apps.size()) {
            return false;
        }
        for (int i = 0; i < apps.size(); i++) {
            WebAppConfig webAppConfig = apps.get(i);
            WebAppConfig currentlyRunningConfig = currentlyRunningApps.get(i);
            if(!webAppConfig.contextPath.equals(currentlyRunningConfig.contextPath) || !webAppConfig.moduleRoot.equals(currentlyRunningConfig.moduleRoot)) {
                return false;
            }
        }
        return true;
    }

    public void stop() {
        try {
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    private String getWebXml(String moduleRoot) {
        if (SpringProfile.activeProfile().equals("it")) {
            // IT-profile: stubbed deps etc
            final String itProfileWebXml = moduleRoot + "/src/test/resources/it-profile-web.xml";
            if (new File(itProfileWebXml).exists()) return itProfileWebXml;
        }
        if (SpringProfile.activeProfile().equals("vagrant")) {
            // Vagrant-profile: use everything from vagrant
            final String vagrantProfileWebXml = moduleRoot + "/src/test/resources/vagrant-profile-web.xml";
            if (new File(vagrantProfileWebXml).exists()) return vagrantProfileWebXml;
        }
        // Other profile: just disable Spring security
        final String testWebXml = moduleRoot + "/src/test/resources/test-web.xml";
        if (new File(testWebXml).exists()) return testWebXml;

        final String defaultWebXml = moduleRoot + "/src/main/webapp/WEB-INF/web.xml";
        if (new File(defaultWebXml).exists()) return defaultWebXml;

        throw new RuntimeException("Could not find web.xml");
    }

    private void setInitialContext(String moduleRoot, Context webContext) {
        if (SpringProfile.activeProfile().equals("vagrant")) {
            final File vagrantContext = new File(moduleRoot + "/src/test/resources/vagrant-context.xml");
            if (vagrantContext.isFile()) {
                try {
                    webContext.setConfigFile(vagrantContext.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public class WebAppConfig {
        String moduleRoot;
        String contextPath;
        Context ctx;
        String webappDirLocation;

        WebAppConfig(String moduleRoot, String contextPath) {
            this.moduleRoot = moduleRoot;
            this.contextPath = contextPath;
        }

        public Context getContext() {
            return this.ctx;
        }
    }
}
