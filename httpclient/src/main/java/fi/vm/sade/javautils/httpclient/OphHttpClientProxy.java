package fi.vm.sade.javautils.httpclient;

public abstract class OphHttpClientProxy extends OphRequestParameterAccessors<OphHttpClientProxy> implements AutoCloseable {
    public abstract OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters);
}
