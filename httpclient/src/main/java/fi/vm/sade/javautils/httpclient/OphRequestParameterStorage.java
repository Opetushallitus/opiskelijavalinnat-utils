package fi.vm.sade.javautils.httpclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        requestParameters.headers.add(new OphHeader(key, value));
        return thisParams;
    }

    public OphRequestParameters getRequestParameters() {
        return requestParameters;
    }

    public OphRequestParameters cloneRequestParameters() {
        return requestParameters.cloneParameters();
    }

    public static class OphRequestParameters implements Cloneable {
        public String clientSubSystemCode = null;
        public List<Integer> expectStatus = new ArrayList<>();
        public OphRequestPostWriter dataWriter = null;
        public String contentType;
        public String dataWriterCharset;
        public List<OphHeader> headers = new ArrayList<>();
        public List<String> acceptMediaTypes = new ArrayList<>();

        public OphRequestParameters cloneParameters() {
            try {
                OphRequestParameters clone = (OphRequestParameters) super.clone();
                clone.expectStatus = new ArrayList<>(clone.expectStatus);
                clone.headers = new ArrayList<>(clone.headers);
                clone.acceptMediaTypes = new ArrayList<>(clone.acceptMediaTypes);
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class OphHeader {
        public final String key, value;

        public OphHeader(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
