package fi.vm.sade.javautils.httpclient;

public abstract class OphHttpClientProxy extends OphRequestParameterAccessors<OphHttpClientProxy> implements AutoCloseable {
    abstract OphHttpClientProxyRequest createRequest(OphRequestParameters requestParameters);
}
