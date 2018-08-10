package fi.vm.sade.javautils.http;

import com.google.gson.Gson;
import fi.vm.sade.javautils.http.exceptions.UnhandledHttpStatusCodeException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static fi.vm.sade.javautils.httpclient.OphHttpClient.Header.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public class OphHttpResponseImpl<T> implements OphHttpResponse<T> {

    private final Gson gson;

    private final CloseableHttpResponse response;
    private final Type returnType;

    private Set<OphHttpCallBack<T>> ophHttpCallBackSet;

    private final String responseMessage;

    OphHttpResponseImpl(CloseableHttpResponse response, Gson gson, Type returnType) {
        this.response = response;
        this.gson = gson;
        this.ophHttpCallBackSet = new HashSet<>();
        this.returnType = returnType;

        try (InputStream inputStream = response.getEntity().getContent()) {
            this.responseMessage = toString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    private int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    private void close() {
        try {
            response.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static String toString(InputStream stream) throws IOException { // IO
        BufferedInputStream bis = new BufferedInputStream(stream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result;
        result = bis.read();
        while(result != -1) {
            buf.write((byte) result);
            result = bis.read();
        }
        return buf.toString();
    }

    @Override
    public OphHttpCallBack<T> handleErrorStatus(int... statusArray) {
        if (this.ophHttpCallBackSet == null) {
            this.ophHttpCallBackSet = new HashSet<>();
        }
        OphHttpCallBack<T> ophHttpCallBack = new OphHttpCallBackImpl<>(statusArray, this);
        this.ophHttpCallBackSet.add(ophHttpCallBack);
        return ophHttpCallBack;
    }

    @Override
    public Optional<T> expectedStatus(int... statusArray) {
        // Expected status code received
        if (Arrays.stream(statusArray).anyMatch(status -> status == this.getStatusCode()) ) {
            return this.convertJsonToObject();
        }
        // Handled error code received
        Optional<OphHttpCallBack<T>> callBackOptional = this.ophHttpCallBackSet.stream()
                .filter(ophHttpCallBack -> ((OphHttpCallBackImpl<T>)ophHttpCallBack).getStatusCode()
                        .contains(this.response.getStatusLine().getStatusCode()))
                .findFirst();
        // If user has not handled 404 assume it means empty resource content.
        if (!callBackOptional.isPresent() && this.response.getStatusLine().getStatusCode() == SC_NOT_FOUND) {
            return Optional.empty();
        }
        return callBackOptional
                .map(ophHttpCallBack -> ((OphHttpCallBackImpl<T>)ophHttpCallBack).getCallBack())
                .orElseThrow(() -> new UnhandledHttpStatusCodeException(this.responseMessage))
                .apply(this.responseMessage);
    }

    @SuppressWarnings("unchecked")
    private Optional<T> convertJsonToObject() {
        T object;
        if (Void.class.getCanonicalName().equals(this.returnType.getTypeName())) {
            object = null;
        }
        else if (String.class.getCanonicalName().equals(this.returnType.getTypeName())
                && this.responseHasContentType(ContentType.TEXT_PLAIN)) {
            object = (T)this.responseMessage;
        }
        else if (this.responseHasContentType(ContentType.APPLICATION_JSON)) {
            object = this.gson.fromJson(this.responseMessage, this.returnType);
        }
        else {
            throw new IllegalStateException("Unsupported content type.");
        }
        return Optional.ofNullable(object);
    }

    private boolean responseHasContentType(ContentType contentType) {
        return Arrays.stream(response.getHeaders(CONTENT_TYPE))
                .anyMatch(header -> header.getValue().startsWith(contentType.getMimeType()));
    }
}
