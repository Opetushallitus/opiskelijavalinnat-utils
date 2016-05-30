package fi.vm.sade.javautils.httpclient;

import fi.vm.sade.properties.OphProperties;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A configurable request object.
 * Once execute() or handleManually() is called OphHttpRequest instance can't be modified.
 * The same OphHttpRequest object can be used to make multiple requests to the same URL.
 */
public class OphHttpRequest extends OphRequestParameterStorage<OphHttpRequest> {
    private final OphProperties properties;
    private OphHttpClientProxy client;

    public OphHttpRequest(OphProperties properties, OphRequestParameters ophRequestParameters, OphHttpClientProxy client) {
        this.properties = properties;
        this.client = client;
        setThisForRequestParamSetters(this);
        setRequestParameters(ophRequestParameters);
    }

    /**
     * Make a request and use handler to handle the response.
     * All resources are automatically released after the handler code finishes.
     * @param handler
     * @param <R>
     * @return
     */
    public <R> R execute(final OphHttpResponseHandler<R> handler) {
        prepareRequest();
        final OphRequestParameters requestParameters = getRequestParameters();
        return handleRetryOnError(requestParameters.method + " " + requestParameters.url, requestParameters.maxRetryCount, requestParameters.retryDelayMs, new CallableWithoutException<R>() {
            @Override
            public R call() {
                try {
                    return client.createRequest(requestParameters).execute(new OphHttpResponseHandler<R>() {
                        @Override
                        public R handleResponse(OphHttpResponse response) throws IOException {
                            verifyResponse(response);
                            return handler.handleResponse(response);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Error handling url: " + requestParameters.url, e);
                }
            }
        });
    }

    /**
     * Make a request and use #expectStatus and #accept() to verify that returned content was ok.
     * All resources are automatically released.
     */
    public void execute() {
        execute(new OphHttpResponseHandler<Void>() {
            @Override
            public Void handleResponse(OphHttpResponse response) {
                return null;
            }
        });
    }

    /**
     * Make a request and handle everything manually. There should not be the need to use this method.
     * @return
     * @throws IOException
     */
    public OphHttpResponse handleManually() throws IOException {
        prepareRequest();
        final OphRequestParameters requestParameters = getRequestParameters();
        return handleRetryOnError(requestParameters.method + " " + requestParameters.url, requestParameters.maxRetryCount, requestParameters.retryDelayMs, new CallableWithoutException<OphHttpResponse>() {
            @Override
            public OphHttpResponse call() {
                OphHttpResponse response;
                try {
                    response = client.createRequest(requestParameters).execute();
                } catch (IOException e) {
                    throw new RuntimeException("Error handling url: " + requestParameters.url, e);
                }
                verifyResponse(response);
                return response;
            }
        });
    }

    private void prepareRequest() {
        if(isEditMode()) {
            final OphRequestParameters requestParameters = getRequestParameters();
            if(requestParameters.dataWriter != null) {
                String contentType = requestParameters.contentType;
                if(!contentType.contains("charset")) {
                    contentType += "; charset=" + requestParameters.dataWriterCharset;
                }
                header(OphHttpClient.Header.CONTENT_TYPE, contentType);
            }
            if(!OphHttpClient.CSRF_SAFE_VERBS.contains(requestParameters.method)) {
                header(OphHttpClient.Header.CSRF, OphHttpClient.Header.CSRF);
            }
            if(requestParameters.acceptMediaTypes.size() > 0) {
                header(OphHttpClient.Header.ACCEPT, join(requestParameters.acceptMediaTypes, ", "));
            }
            if(requestParameters.clientSubSystemCode != null) {
                header("clientSubSystemCode", requestParameters.clientSubSystemCode);
            }
            if(requestParameters.url == null) {
                requestParameters.url = createUrl(requestParameters);
            }
            disableEditMode();
        }
    }

    private String createUrl(OphRequestParameters requestParameters) {
        Object params[] = requestParameters.urlParams;
        if(requestParameters.params.size() > 0) {
            params = appendElementToArray(params, requestParameters.params);
        }
        return properties.url(requestParameters.urlKey, params);
    }

    private static <R> R[] appendElementToArray(final R[] a, final R e) {
        R[] copy  = Arrays.copyOf(a, a.length + 1);
        copy[copy.length - 1] = e;
        return copy;
    }


    private void verifyResponse(OphHttpResponse response) {
        OphRequestParameters requestParameters = getRequestParameters();
        String url = requestParameters.url;

        boolean statusOk;
        int status = response.getStatusCode();
        if(requestParameters.expectStatus.size() == 0) {
            statusOk = status >= 200 && status < 300;
        } else {
            statusOk = requestParameters.expectStatus.contains(status);
        }

        if (!statusOk) {
            String expected;
            if(requestParameters.expectStatus.size() == 0) {
                expected = "any 2xx code";
            } else if(requestParameters.expectStatus.size() == 1){
                expected = requestParameters.expectStatus.get(0).toString();
            } else {
                expected = "any of " + join(requestParameters.expectStatus, ", ");
            }
            throw new RuntimeException("Unexpected response status: " + status + " Url: " + url + " Expected: " + expected);
        }

        if(requestParameters.acceptMediaTypes.size() > 0) {
            String error = null;
            List<String> responseContentTypeHeaders = response.getHeaders(OphHttpClient.Header.CONTENT_TYPE);
            if( responseContentTypeHeaders.size() == 0) {
                error = "header is missing";
            } else if(responseContentTypeHeaders.size() == 1){
                String s = responseContentTypeHeaders.get(0);
                if(!matchesAny(s, requestParameters.acceptMediaTypes)) {
                    error = "value " + s;
                }
            } else {
                error = "returned " + responseContentTypeHeaders.size() + " headers when expected one. Values: " + join(responseContentTypeHeaders, ", ");
            }
            if(error != null) {
                throw new RuntimeException("Error with response " + OphHttpClient.Header.CONTENT_TYPE + " header. Url: "+ url +" Error: " + error + " Expected: " + join(requestParameters.acceptMediaTypes, ", "));
            }
        }
    }

    private static boolean matchesAny(String s, List<String> acceptMediaTypes) {
        for(String a: acceptMediaTypes) {
            if(a.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static <V> V handleRetryOnError(String id, Integer maxCount, Integer delayMs, CallableWithoutException<V> callable) {
        if(maxCount != null) {
            int count = 0;
            while(true) {
                try {
                    return callable.call();
                } catch(Exception e) {
                    ++count;
                    if(maxCount != -1 && maxCount == count) {
                        throw new RuntimeException("Tried " + id + " " + count + " times", e);
                    }
                    if(delayMs != null && delayMs > 0) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e1) {
                            throw new RuntimeException("Interrupted: " + id, e1);
                        }
                    }
                }
            }
        } else {
            return callable.call();
        }
    }

    public static String join(Collection col, String sep) {
        StringBuilder buf = new StringBuilder();
        boolean firstDone = false;
        for(Object o: col) {
            if(firstDone) {
                buf.append(sep);
            } else {
                firstDone = true;
            }
            buf.append(o.toString());
        }
        return buf.toString();
    }
}

