package fi.vm.sade.javautils.nio.cas.impl;

import fi.vm.sade.javautils.nio.cas.CasConfig;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.asynchttpclient.RequestBuilder;

public class CasUtils {
    private final CasConfig config;
    public CasUtils(CasConfig config) {
        this.config = config;
    }

    public RequestBuilder withCallerIdAndCsrfHeader(RequestBuilder requestBuilder) {
        return requestBuilder.setHeader("Caller-Id", config.getCallerId())
                .setHeader("CSRF", config.getCsrf())
                .addOrReplaceCookie(new DefaultCookie("CSRF", config.getCsrf()));
    }

    public RequestBuilder withCallerIdAndCsrfHeader() {
        return withCallerIdAndCsrfHeader(new RequestBuilder());
    }

    public RequestBuilder withTicket(RequestBuilder requestBuilder, String ticket) {
        if (this.config.getServiceTicketHeaderName() == null) {
            return requestBuilder.addQueryParam("ticket", ticket);
        } else {
            return requestBuilder.addHeader(this.config.getServiceTicketHeaderName(), ticket);
        }
    }
}
