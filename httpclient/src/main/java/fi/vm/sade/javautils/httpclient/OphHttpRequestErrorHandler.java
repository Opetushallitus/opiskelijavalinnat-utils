package fi.vm.sade.javautils.httpclient;

public interface OphHttpRequestErrorHandler<R> {
    R handleError(OphRequestParameters requestParameters, OphHttpResponse response, RuntimeException exception);
}
