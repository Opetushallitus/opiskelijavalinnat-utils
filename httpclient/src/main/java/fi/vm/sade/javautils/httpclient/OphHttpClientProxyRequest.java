package fi.vm.sade.javautils.httpclient;

public interface OphHttpClientProxyRequest {
    OphHttpResponse execute();
    <R> R execute(OphHttpResponseHandler<? extends R> handler);
}
