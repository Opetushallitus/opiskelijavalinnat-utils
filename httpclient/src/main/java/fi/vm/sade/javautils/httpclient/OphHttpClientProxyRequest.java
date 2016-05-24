package fi.vm.sade.javautils.httpclient;

import java.io.IOException;

public interface OphHttpClientProxyRequest {
    OphHttpResponse execute() throws IOException;
    <R> R execute(OphHttpResponseHandler<? extends R> handler) throws IOException;
}
