package fi.vm.sade.javautils.nio.cas;

import com.google.gson.Gson;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

public class CasClientHelper {

    private static final String HTTP_METHOD_DELETE = "DELETE";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String HTTP_METHOD_PUT = "PUT";

    private final CasClient client;
    private final Gson gson;

    public CasClientHelper(CasClient client) {
        this.client = client;
        this.gson = new Gson();
    }

    public CasClientHelper(CasClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    private Request createRequest(String method, String url) {
        return createRequest(method, url, null);
    }

    private <R> Request createRequest(String method, String url, R requestEntity) {
        RequestBuilder requestBuilder = new RequestBuilder()
                .setMethod(method)
                .setUrl(url);

        if (requestEntity != null) {
            String json = gson.toJson(requestEntity);
            requestBuilder = requestBuilder.setBody(json);
        }

        return requestBuilder.build();
    }

    private <T> T parseJson(Response response, Class<T> responseTypeClass) {
        return gson.fromJson(response.getResponseBody(), responseTypeClass);
    }

    private <T> T executeSyncWithType(Request request, Class<T> responseTypeClass) {
        try {
            return executeWithType(request, responseTypeClass).get();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Pyyntö epäonnistui | Request: %s", request), e);
        }
    }

    private <T> CompletableFuture<T> executeWithType(Request request, Class<T> responseTypeClass) {
        return execute(request).thenApplyAsync(response -> parseJson(response, responseTypeClass));
    }

    private Response executeSync(Request request) {
        try {
            return execute(request).get();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Pyyntö epäonnistui | Request: %s", request), e);
        }
    }

    private CompletableFuture<Response> execute(Request request) {
        return client.execute(request);
    }

    public <T> T doGetSync(String url, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_GET, url);
        return executeSyncWithType(request, responseTypeClass);
    }

    public <T> CompletableFuture<T> doGet(String url, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_GET, url);
        return executeWithType(request, responseTypeClass);
    }

    public Response doGetSync(String url) {
        Request request = createRequest(HTTP_METHOD_GET, url);
        return executeSync(request);
    }

    public CompletableFuture<Response> doGet(String url) {
        Request request = createRequest(HTTP_METHOD_GET, url);
        return execute(request);
    }

    public <R, T> T doPostSync(String url, R requestEntity, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_POST, url, requestEntity);
        return executeSyncWithType(request, responseTypeClass);
    }

    public <R, T> CompletableFuture<T> doPost(String url, R requestEntity, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_POST, url, requestEntity);
        return executeWithType(request, responseTypeClass);
    }

    public <R> Response doPostSync(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_POST, url, requestEntity);
        return executeSync(request);
    }

    public <R> CompletableFuture<Response> doPost(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_POST, url, requestEntity);
        return execute(request);
    }

    public <R, T> T doPutSync(String url, R requestEntity, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_PUT, url, requestEntity);
        return executeSyncWithType(request, responseTypeClass);
    }

    public <R, T> CompletableFuture<T> doPut(String url, R requestEntity, Class<T> responseTypeClass) {
        Request request = createRequest(HTTP_METHOD_PUT, url, requestEntity);
        return executeWithType(request, responseTypeClass);
    }

    public <R> Response doPutSync(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_PUT, url, requestEntity);
        return executeSync(request);
    }

    public <R> CompletableFuture<Response> doPut(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_PUT, url, requestEntity);
        return execute(request);
    }

    public <R> Response doDeleteSync(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_DELETE, url, requestEntity);
        return executeSync(request);
    }

    public <R> CompletableFuture<Response> doDelete(String url, R requestEntity) {
        Request request = createRequest(HTTP_METHOD_DELETE, url, requestEntity);
        return execute(request);
    }

    public Response doDeleteSync(String url) {
        Request request = createRequest(HTTP_METHOD_DELETE, url);
        return executeSync(request);
    }

    public CompletableFuture<Response> doDelete(String url) {
        Request request = createRequest(HTTP_METHOD_DELETE, url);
        return execute(request);
    }
}
