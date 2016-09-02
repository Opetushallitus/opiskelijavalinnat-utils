package fi.vm.sade.javautils.httpclient;

import java.util.Arrays;

class OphRequestParameterAccessors<T> {
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

    public T dataWriter(String contentType, String encoding, OphRequestPostWriter writer) {
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
    public T param(String key, Object... values) {
        checkEditMode();
        for(Object value: values) {
            requestParameters.params.add(key, value.toString());
        }
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

    public T retryOnError(Integer maxRetryCount, Integer retryDelayMs) {
        checkEditMode();
        requestParameters.maxRetryCount = maxRetryCount;
        requestParameters.retryDelayMs = retryDelayMs;
        return thisParams;
    }

    public T retryOnError(Integer maxRetryCount) {
        checkEditMode();
        requestParameters.maxRetryCount = maxRetryCount;
        return thisParams;
    }

    public T skipResponseAssertions() {
        checkEditMode();
        requestParameters.skipResponseAssertions = true;
        return thisParams;
    }

    /**
     * Add errorHandler that is called when an exception is thrown.
     *
     * note1: It should either throw an exception or return an object of correct type (= of the same type as execute returns). Otherwise you'll get a class cast exception if the code uses the returned value.
     *
     * note 2: errorHandler.handleError() needs to handle cases where response is null.
     *
     * note 3: If your execute method throws an exception by itself, onError will catch it.
     *
     * note 4: Per request, there can be only one onError or recover handler.
     *
     * @param errorHandler
     * @return
     */
    public <R> T onError(OphHttpRequestErrorHandler<R> errorHandler) {
        checkEditMode();
        requestParameters.onError = errorHandler;
        return thisParams;
    }

    public T throwOnlyOnErrorExceptions() {
        checkEditMode();
        requestParameters.throwOnlyOnErrorExceptions = true;
        return thisParams;
    }

    public T doNotSendOphHeaders() {
        checkEditMode();
        requestParameters.sendOphHeaders = false;
        return thisParams;
    }
}
