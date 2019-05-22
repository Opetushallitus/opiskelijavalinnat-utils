package fi.vm.sade.javautils.httpclient;

import fi.vm.sade.properties.OphProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

/**
 * A configurable request object.
 * Once execute() or handleManually() is called OphHttpRequest instance can't be modified.
 * The same OphHttpRequest object can be used to make multiple requests to the same URL.
 */
public class OphHttpRequest extends OphRequestParameterAccessors<OphHttpRequest> {
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
     */
    public <R> R execute(final OphHttpResponseHandler<R> handler) {
        prepareRequest();
        final OphRequestParameters requestParameters = getRequestParameters();
        final OphHttpResponse[] responseForOnError = new OphHttpResponse[1];
        return handleOnError(requestParameters, responseForOnError, () -> {
            return handleRetryOnError(requestParameters.method + " " + requestParameters.url, requestParameters.maxRetryCount, requestParameters.retryDelayMs, () -> {
                try {
                    return client.createRequest(requestParameters).execute(response -> {
                        responseForOnError[0] = response;
                        checkResponse(response);
                        return handler.handleResponse(response);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Error handling url: " + requestParameters.url, e);
                }
            });
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
     */
    public OphHttpResponse handleManually() throws IOException {
        prepareRequest();
        final OphRequestParameters requestParameters = getRequestParameters();
        final OphHttpResponse[] responseForOnError = new OphHttpResponse[1];
        return handleOnError(requestParameters, responseForOnError, () -> {
            return handleRetryOnError(requestParameters.method + " " + requestParameters.url, requestParameters.maxRetryCount, requestParameters.retryDelayMs, () -> {
                OphHttpResponse response;
                try {
                    response = client.createRequest(requestParameters).handleManually();
                } catch (IOException e) {
                    throw new RuntimeException("Error handling url: " + requestParameters.url, e);
                }
                checkResponse(response);
                return response;
            });
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
                header(CONTENT_TYPE, contentType);
            }
            if(requestParameters.acceptMediaTypes.size() > 0) {
                header(OphHttpClient.Header.ACCEPT, join(requestParameters.acceptMediaTypes, ", "));
            }
            if(requestParameters.sendOphHeaders) {
                if(!OphHttpClient.CSRF_SAFE_VERBS.contains(requestParameters.method)) {
                    header(OphHttpClient.Header.CSRF, OphHttpClient.Header.CSRF);
                }
                if(requestParameters.callerId != null) {
                    header("Caller-Id", requestParameters.callerId);
                }
                }
            if(requestParameters.url == null) {
                requestParameters.url = createUrl(requestParameters);
            }
            disableEditMode();
        }
    }

    private String createUrl(OphRequestParameters requestParameters) {
        List<Object> params = new ArrayList<>(Arrays.asList(requestParameters.urlParams));
        if(requestParameters.params.size() > 0) {
            params.add(requestParameters.params);
        }
        return properties.url(requestParameters.urlKey, params.toArray(new Object[params.size()]));
    }

    private void checkResponse(OphHttpResponse response) {
        OphRequestParameters requestParameters = getRequestParameters();
        if(!requestParameters.skipResponseAssertions) {
            String url = requestParameters.url;
            verifyStatusCode(response, requestParameters.expectStatus, url);
            verifyContentType(response, requestParameters.acceptMediaTypes, url);
        }
    }

    private void verifyContentType(OphHttpResponse response, List<String> acceptMediaTypes, String url) {
        if(acceptMediaTypes != null && acceptMediaTypes.size() > 0) {
            String headerKey = CONTENT_TYPE;
            String headerValue = getSingleHeaderValue(response, headerKey);
            int i = headerValue.indexOf(";");
            if(i > -1) {
                headerValue = headerValue.substring(0,i);
            }
            headerValue = headerValue.trim();
            if(!matchesAny(headerValue, acceptMediaTypes)) {
                throw new RuntimeException("Error with response " + headerKey + " header. Url: "+ url +" Error value: " + headerValue + " Expected: " + join(acceptMediaTypes, ", "));
           }
        }
    }

    private void verifyStatusCode(OphHttpResponse response, List<Integer> expectStatus, String url) {
        boolean statusOk;
        int status = response.getStatusCode();
        if(expectStatus.size() == 0) {
            statusOk = status >= 200 && status < 300;
        } else {
            statusOk = expectStatus.contains(status);
        }

        if (!statusOk) {
            String expected;
            if(expectStatus.size() == 0) {
                expected = "any 2xx code";
            } else if(expectStatus.size() == 1){
                expected = expectStatus.get(0).toString();
            } else {
                expected = "any of " + join(expectStatus, ", ");
            }
            throw new RuntimeException("Unexpected response status: " + status + " Expected: " + expected + " Url: " + url);
        }
    }

    private String getSingleHeaderValue(OphHttpResponse response, String headerKey) {
        List<String> values = response.getHeaderValues(headerKey);
        if( values.size() == 0 || values.size() > 1) {
            OphRequestParameters requestParameters = getRequestParameters();
            String url = requestParameters.url;
            throw new RuntimeException("Expected response for url " + url + " to include header " + headerKey + " once. There was " + values.size() + " headers.");
        }
        return values.get(0);
    }

    private static boolean matchesAny(String s, List<String> acceptMediaTypes) {
        for(String a: acceptMediaTypes) {
            if(a.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private <V> V handleOnError(final OphRequestParameters requestParameters, final OphHttpResponse[] responseForOnError, CallableWithoutException<V> callable) {
        try {
            return callable.call();
        } catch (RuntimeException exceptionFromCode) {
            Object ret = null;
            RuntimeException exceptionFromOnError = null;
            if( requestParameters.onError != null) {
                try {
                    ret = requestParameters.onError.handleError(requestParameters, responseForOnError[0], exceptionFromCode);
                } catch (RuntimeException e) {
                    exceptionFromOnError = e;
                }
            }
            if(requestParameters.throwOnlyOnErrorExceptions) {
                if(exceptionFromOnError != null) {
                    if(exceptionFromCode == exceptionFromOnError) {
                        throw exceptionFromCode;
                    } else {
                        throw new RuntimeException("Http request failed and onError handler failed also! exceptionFromOnError.getMessage() is: " + exceptionFromOnError.getMessage()+ " Exception from http request follows", exceptionFromCode);
                    }
                }
                return (V) ret;
            } else {
                throw exceptionFromCode;
            }
        }
    }

    private static <V> V handleRetryOnError(String id, Integer maxCount, Integer delayMs, CallableWithoutException<V> callable) {
        if(shouldRetryOnError(maxCount)) {
            int count = 0;
            while(true) {
                try {
                    return callable.call();
                } catch(Exception e) {
                    if(maxCount == ++count) {
                        throw new RuntimeException("Tried " + count + " times " + id, e);
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
        }

        return callable.call();
    }

    private static boolean shouldRetryOnError(Integer maxCount) {
        return maxCount != null && maxCount > 0;
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

