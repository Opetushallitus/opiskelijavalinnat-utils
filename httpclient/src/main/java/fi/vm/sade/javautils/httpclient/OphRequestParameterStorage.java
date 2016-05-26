package fi.vm.sade.javautils.httpclient;

import java.util.Arrays;

public class OphRequestParameterStorage<T> {
    private T thisParams;
    private OphRequestParameters requestParameters = new OphRequestParameters();
    private boolean editMode = true;

    void setThisForRequestParamSetters(T paramsThis) {
        this.thisParams = paramsThis;
    }

    public void setRequestParameters(OphRequestParameters requestParameters) {
        checkEditMode();
        this.requestParameters = requestParameters;
    }

    public T setClientSubSystemCode(String clientSubSystemCode) {
        checkEditMode();
        requestParameters.clientSubSystemCode = clientSubSystemCode;
        return thisParams;
    }

    public T expectStatus(Integer... statusCodes) {
        checkEditMode();
        requestParameters.expectStatus = Arrays.asList(statusCodes);
        return thisParams;
    }

    public T accept(String... mediaTypes) {
        checkEditMode();
        requestParameters.acceptMediaTypes.addAll(Arrays.asList(mediaTypes));
        return thisParams;
    }

    public T data(String contentType, String encoding, OphRequestPostWriter writer) {
        checkEditMode();
        requestParameters.contentType = contentType;
        requestParameters.dataWriterCharset = encoding;
        requestParameters.dataWriter = writer;
        return thisParams;
    }

    public T header(String key, String value) {
        checkEditMode();
        requestParameters.headers.add(key, value);
        return thisParams;
    }

    /**
     * Add named path parameter, if it's not used as a named path parameter use it as querystring parameter
     */
    public T param(String key, Object value) {
        checkEditMode();
        requestParameters.params.add(key, value.toString());
        return thisParams;
    }

    public T disableEditMode() {
        editMode = false;
        return thisParams;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public OphRequestParameters getRequestParameters() {
        return requestParameters;
    }

    public OphRequestParameters cloneRequestParameters() {
        return requestParameters.cloneParameters();
    }

    private void checkEditMode() {
        if(!editMode) {
            throw new RuntimeException("Request parameters are not modifiable");
        }
    }

}
