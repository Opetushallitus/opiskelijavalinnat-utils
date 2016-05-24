package fi.vm.sade.javautils.httpclient;

import java.util.Arrays;

public class OphRequestParameterStorage<T> {
    private T thisParams;
    private OphRequestParameters requestParameters = new OphRequestParameters();

    void setThisForRequestParamSetters(T paramsThis) {
        this.thisParams = paramsThis;
    }

    public void setRequestParameters(OphRequestParameters requestParameters) {
        this.requestParameters = requestParameters;
    }

    public T setClientSubSystemCode(String clientSubSystemCode) {
        requestParameters.clientSubSystemCode = clientSubSystemCode;
        return thisParams;
    }

    public T expectStatus(Integer... statusCodes) {
        requestParameters.expectStatus = Arrays.asList(statusCodes);
        return thisParams;
    }

    public T accept(String... mediaTypes) {
        requestParameters.acceptMediaTypes.addAll(Arrays.asList(mediaTypes));
        return thisParams;
    }

    public T data(String contentType, String encoding, OphRequestPostWriter writer) {
        requestParameters.contentType = contentType;
        requestParameters.dataWriterCharset = encoding;
        requestParameters.dataWriter = writer;
        return thisParams;
    }

    public T header(String key, String value) {
        requestParameters.headers.add(key, value);
        return thisParams;
    }

    /**
     * Add named path parameter, if it's not used as a named path parameter use it as querystring parameter
     */
    public T param(String key, String value) {
        requestParameters.params.add(key, value);
        return thisParams;
    }

    public OphRequestParameters getRequestParameters() {
        return requestParameters;
    }

    public OphRequestParameters cloneRequestParameters() {
        return requestParameters.cloneParameters();
    }

}
