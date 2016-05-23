package fi.vm.sade.javautils.httpclient;

public abstract class OphHttpClientProxy extends OphRequestParameterStorage<OphHttpClientProxy> {
    abstract OphHttpClientProxyRequest createRequest(String method, OphRequestParameters requestParameters, String url);
}
