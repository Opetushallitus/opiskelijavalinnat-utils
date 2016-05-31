package fi.vm.sade.javautils.httpclient;

import java.io.IOException;

public interface OphHttpClientProxyRequest {
    <R> R execute(OphHttpResponseHandler<? extends R> handler) throws IOException;

    /**
     * Should not be used. Use execute() instead because it closes connection automatically.
     * @return
     * @throws IOException
     */
    OphHttpResponse handleManually() throws IOException;
}
