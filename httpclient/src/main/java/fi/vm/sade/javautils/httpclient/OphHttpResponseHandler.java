package fi.vm.sade.javautils.httpclient;

public interface OphHttpResponseHandler<R> {
    R handleResponse(OphHttpResponse response) throws Exception;
}
