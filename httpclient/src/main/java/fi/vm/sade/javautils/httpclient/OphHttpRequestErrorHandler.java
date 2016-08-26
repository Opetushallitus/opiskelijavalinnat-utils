package fi.vm.sade.javautils.httpclient;

public interface OphHttpRequestErrorHandler {
    Object handleError(OphRequestParameters requestParameters, OphHttpResponse response, RuntimeException exception);
}
