package fi.vm.sade.javautils.httpclient;

public abstract class OphHttpClientProxy extends OphRequestParameterStorage<OphHttpClientProxy> {
    abstract OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters);
}
