package fi.vm.sade.javautils.httpclient;

import java.util.List;

public class OphHttpRequest extends OphRequestParameterStorage<OphHttpRequest> {
    private final String url;
    private final String method;
    private OphHttpClientProxy client;

    public OphHttpRequest(String method, String url, OphRequestParameters ophRequestParameters, OphHttpClientProxy client) {
        this.method = method;
        this.url = url;
        this.client = client;
        setThisForRequestParamSetters(this);
        setRequestParameters(ophRequestParameters);
    }

    public OphHttpResponse handleManually() {
        prepareRequest();
        OphHttpResponse response = client.createRequest(method, getRequestParameters(), url).execute();
        verifyResponse(response);
        return response;
    }

    public void execute() {
        execute(new OphHttpResponseHandler<Void>() {
            @Override
            public Void handleResponse(OphHttpResponse response) {
                return null;
            }
        });
    }

    public <R> R execute(final OphHttpResponseHandler<? extends R> handler) {
        prepareRequest();
        return client.createRequest(method, getRequestParameters(), url).execute(new OphHttpResponseHandler<R>() {
            @Override
            public R handleResponse(OphHttpResponse response) {
                verifyResponse(response);
                return handler.handleResponse(response);
            }
        });
    }

    private void prepareRequest() {
        final OphRequestParameters requestParameters = getRequestParameters();
        if(requestParameters.dataWriter != null) {
            String contentType = requestParameters.contentType;
            if(!contentType.contains("charset")) {
                contentType += "; charset=" + requestParameters.dataWriterCharset;
            }
            header(OphHttpClient.Header.CONTENT_TYPE, contentType);
        }
        if(!OphHttpClient.CSRF_SAFE_VERBS.contains(method)) {
            header(OphHttpClient.Header.CSRF, OphHttpClient.Header.CSRF);
        }
        if(requestParameters.acceptMediaTypes.size() > 0) {
            header(OphHttpClient.Header.ACCEPT, OphHttpClient.join(requestParameters.acceptMediaTypes, ", "));
        }
        if(requestParameters.clientSubSystemCode != null) {
            header("clientSubSystemCode", requestParameters.clientSubSystemCode);
        }
    }

    private void verifyResponse(OphHttpResponse response) {
        OphRequestParameters requestParameters = getRequestParameters();

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
                expected = "any of " + OphHttpClient.join(requestParameters.expectStatus, ", ");
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
                error = "returned " + responseContentTypeHeaders.size() + " headers when expected one. Values: " + OphHttpClient.join(responseContentTypeHeaders, ", ");
            }
            if(error != null) {
                throw new RuntimeException("Error with response " + OphHttpClient.Header.CONTENT_TYPE + " header. Url: "+url+" Error: " + error + " Expected: " + OphHttpClient.join(requestParameters.acceptMediaTypes, ", "));
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
}
