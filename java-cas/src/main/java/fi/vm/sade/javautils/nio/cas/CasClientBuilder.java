package fi.vm.sade.javautils.nio.cas;

import fi.vm.sade.javautils.nio.cas.impl.CasClientImpl;
import fi.vm.sade.javautils.nio.cas.impl.CasSessionFetcher;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.util.concurrent.ThreadFactory;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class CasClientBuilder {

    public static CasClient buildFromConfigAndHttpClient(CasConfig config, AsyncHttpClient asyncHttpClient) {
        CasSessionFetcher casSessionFetcher =
        new CasSessionFetcher(
                config,
                asyncHttpClient,
                config.getSessionTicketValidMs(),
                config.getTicketGrantingTicketValidMs());
        return new CasClientImpl(config, asyncHttpClient, casSessionFetcher);
    }

    public static CasClient build(CasConfig config) {
        return buildFromConfigAndHttpClient(config, defaultHttpClient());
    }

    private static AsyncHttpClient defaultHttpClient() {
        ThreadFactory factory = BasicThreadFactory.builder()
                .namingPattern("async-cas-client-thread-%d")
                .daemon(true)
                .priority(Thread.NORM_PRIORITY)
                .build();

        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setThreadFactory(factory)
                .setHttp2Enabled(false)
                .build();

        return asyncHttpClient(config);
    }
}
