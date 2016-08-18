package fi.vm.sade.javautils.httpclient;

public abstract class OphHttpClientProxy extends OphRequestParameterAccessors<OphHttpClientProxy> {
    abstract OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters);
}
