module fi.vm.sade.javautils.cas {
    exports fi.vm.sade.javautils.nio.cas;
    requires async.http.client;
    requires com.google.gson;
    requires io.netty.codec.http;
    requires java.net.http;
    requires java.xml;
    requires org.apache.commons.lang3;
    requires io.github.resilience4j.circuitbreaker;
}
