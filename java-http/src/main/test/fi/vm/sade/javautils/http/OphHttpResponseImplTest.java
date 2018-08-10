package fi.vm.sade.javautils.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.javautils.http.exceptions.UnhandledHttpStatusCodeException;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Optional;

import static fi.vm.sade.javautils.httpclient.OphHttpClient.Header.CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;

public class OphHttpResponseImplTest {
    @Test
    public void testString() {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 200, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        String string = ophHttpResponse.expectedStatus(200).orElseThrow(RuntimeException::new);
        assertEquals(string, "replystring");
    }

    @Test
    public void testStringErrorHandling() {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 400, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        String string = ophHttpResponse
                .handleErrorStatus(400).with(Optional::ofNullable)
                .expectedStatus(200).orElseThrow(RuntimeException::new);
        assertEquals(string, "replystring");
    }

    @Test(expected = UnhandledHttpStatusCodeException.class)
    public void unhandledStatusCode() {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 400, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        ophHttpResponse
                .handleErrorStatus(401).with(Optional::ofNullable)
                .expectedStatus(200);
    }

    @Test
    public void testStringContainingJson() {
        CloseableHttpResponse httpResponse = this.mockResponse("{}", 200, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        String string = ophHttpResponse.expectedStatus(200).orElseThrow(RuntimeException::new);
        assertEquals(string, "{}");
    }

    @Test
    public void testJsonString() {
        CloseableHttpResponse httpResponse = this.mockResponse("\"{}\"", 200, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        String string = ophHttpResponse.expectedStatus(200).orElseThrow(RuntimeException::new);
        assertEquals(string, "{}");
    }

    @Test
    public void testJsonObject() {
        CloseableHttpResponse httpResponse = this.mockResponse("{\"value\":\"stringvalue\"}", 201, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(TestObject.class).getType();
        OphHttpResponse<TestObject> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        TestObject testObject = ophHttpResponse.expectedStatus(201).orElseThrow(RuntimeException::new);
        assertEquals(testObject.getValue(), "stringvalue");
    }

    @Test
    public void testNoResponseJson() {
        CloseableHttpResponse httpResponse = this.mockResponse("{\"value\":\"stringvalue\"}", 201, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(Void.class).getType();
        OphHttpResponse<Void> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        Optional testObject = ophHttpResponse.expectedStatus(201);
        assertFalse(testObject.isPresent());
    }

    @Test
    public void testNoResponseText() {
        CloseableHttpResponse httpResponse = this.mockResponse("\"value\"", 201, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(Void.class).getType();
        OphHttpResponse<Void> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse, new Gson(), type);
        Optional testObject = ophHttpResponse.expectedStatus(201);
        assertFalse(testObject.isPresent());
    }

    @Getter
    @Setter
    static class TestObject {
        String value;
    }

    private CloseableHttpResponse mockResponse(String json, int returnStatus, String contentType) {
        try {
            InputStream inputStream = new ByteArrayInputStream(json.getBytes());
            CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
            HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
            given(httpResponse.getEntity()).willReturn(httpEntity);
            given(httpEntity.getContent()).willReturn(inputStream);

            StatusLine statusLine = Mockito.mock(StatusLine.class);
            given(httpResponse.getStatusLine()).willReturn(statusLine);
            given(statusLine.getStatusCode()).willReturn(returnStatus);

            Header[] headers = {new BasicHeader(CONTENT_TYPE, contentType)};
            given(httpResponse.getHeaders(eq(CONTENT_TYPE))).willReturn(headers);

            return httpResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
