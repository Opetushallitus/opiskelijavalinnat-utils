package fi.vm.sade.javautils.httpclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OphRequestParameters implements Cloneable {

    // Method and url
    public String method = null;
    public String url = null;
    public String urlKey = null;
    public Object[] urlParams = null;
    public MultiValueMap<String, String> params = new MultiValueMap<>();

    // Values for request
    public MultiValueMap<String, String> headers = new MultiValueMap<>();
    public String callerId = null;
    public OphRequestPostWriter dataWriter = null;
    public String contentType = null;
    public String dataWriterCharset = null;

    // Assertions for response
    public List<Integer> expectStatus = new ArrayList<>();
    public List<String> acceptMediaTypes = new ArrayList<>();
    public Integer maxRetryCount  = null;
    public Integer retryDelayMs = null;
    public boolean skipResponseAssertions = false;
    public OphHttpRequestErrorHandler onError;
    public boolean throwOnlyOnErrorExceptions = false;
    public boolean sendOphHeaders = true;

    public OphRequestParameters cloneParameters() {
        try {
            OphRequestParameters clone = (OphRequestParameters) super.clone();
            clone.expectStatus = new ArrayList<>(expectStatus);
            clone.acceptMediaTypes = new ArrayList<>(acceptMediaTypes);
            clone.headers = new MultiValueMap<>();
            clone.params = new MultiValueMap<>();
            clone.headers.putAll(headers);
            clone.params.putAll(params);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class MultiValueMap<K,V> extends HashMap<K,List<V>> {
        public void add(K key, V value) {
            if(containsKey(key)) {
                get(key).add(value);
            } else {
                ArrayList<V> list = new ArrayList<>();
                list.add(value);
                put(key, list);
            }
        }
    }
}
