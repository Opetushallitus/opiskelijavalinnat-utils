package fi.vm.sade.javautils.http;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.javautils.http.exceptions.UnhandledHttpStatusCodeException;
import fi.vm.sade.javautils.http.mappers.GsonConfiguration;
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.javautils.httpclient.OphHttpClient.Header.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OphHttpResponseImplTest {
    @Test
    public void testString() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 200, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        String string = ophHttpResponse.expectedStatus(200)
                .mapWith((response) -> new GsonConfiguration().getGson().fromJson(response, type))
                .orElseThrow(RuntimeException::new);
        assertThat(string).isEqualTo("replystring");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testStringErrorHandling() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 400, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        String string = ophHttpResponse
                .handleErrorStatus(400)
                .with(Optional::ofNullable)
                .expectedStatus(200)
                .mapWith(text -> new GsonConfiguration().getGson()
                        .fromJson(text, type))
                .orElseThrow(RuntimeException::new);
        assertThat(string).isEqualTo("replystring");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void unhandledStatusCode() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 400, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        assertThatThrownBy(() -> ophHttpResponse.handleErrorStatus(401)
                .with(Optional::ofNullable)
                .expectedStatus(200)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type))
        ).isInstanceOf(UnhandledHttpStatusCodeException.class).hasMessage("Invalid status code 400. replystring");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void undefinedErrorHandlerCodeDoesNotHandleAnything() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("replystring", 400, ContentType.TEXT_PLAIN.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        assertThatThrownBy(() -> ophHttpResponse.handleErrorStatus()
                .with(Optional::ofNullable)
                .expectedStatus(200)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type))
        ).isInstanceOf(UnhandledHttpStatusCodeException.class).hasMessage("Invalid status code 400. replystring");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testStringContainingJson() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("{}", 200, ContentType.TEXT_PLAIN.getMimeType());
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        String string = ophHttpResponse.expectedStatus(200)
                .mapWith(text -> text)
                .orElseThrow(RuntimeException::new);
        assertThat(string).isEqualTo("{}");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testJsonString() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("\"{}\"", 200, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(String.class).getType();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        String string = ophHttpResponse.expectedStatus(200)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type))
                .orElseThrow(RuntimeException::new);
        assertThat(string).isEqualTo("{}");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testJsonObject() throws Exception {
        String json = "{\"value\":\"stringvalue\",\"javaDate\":1471333673128,\"sqlDate\":1471333673128, \"localDate\": \"1971-02-10\", \"localDateTime\":\"2014-03-10T18:46:40.000\"}";
        CloseableHttpResponse httpResponse = this.mockResponse(json, 201, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(TestObject.class).getType();
        OphHttpResponse<TestObject> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        TestObject testObject = ophHttpResponse.expectedStatus(201)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type))
                .orElseThrow(RuntimeException::new);
        assertThat(testObject)
                .extracting(TestObject::getValue, TestObject::getJavaDate, TestObject::getSqlDate,
                        TestObject::getLocalDate, TestObject::getLocalDateTime)
                .containsExactly("stringvalue", new Date(1471333673128L), new java.sql.Date(1471333673128L),
                        LocalDate.parse("1971-02-10"), LocalDateTime.parse("2014-03-10T18:46:40.000"));
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testJsonObjectNotFoundOnServer() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("{\"value\":\"stringvalue\"}", 404, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(TestObject.class).getType();
        OphHttpResponse<TestObject> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        Optional<TestObject> testObject = ophHttpResponse.expectedStatus(201)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type));
        assertThat(testObject).isNotPresent();
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testJsonCollection() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("[\"value1\",\"value2\"]", 200, ContentType.APPLICATION_JSON.getMimeType());
        Type type = TypeToken.get(TypeToken.getParameterized(ArrayList.class, String.class).getType()).getType();
        OphHttpResponse<List<String>> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        List<String> stringList = ophHttpResponse.expectedStatus(200)
                .mapWith(text -> new GsonConfiguration().getGson().fromJson(text, type))
                .orElseThrow(RuntimeException::new);
        assertThat(stringList).containsExactly("value1", "value2");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testNoResponseText() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("\"value\"", 201, ContentType.TEXT_PLAIN.getMimeType());
        OphHttpResponse<Void> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        ophHttpResponse.expectedStatus(201)
                .ignoreResponse();
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testNoResponseTextWithoutStatusCode() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("\"value\"", 201, ContentType.TEXT_PLAIN.getMimeType());
        OphHttpResponse<Void> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        assertThatThrownBy(() -> ophHttpResponse.expectedStatus().ignoreResponse())
                .isInstanceOf(UnhandledHttpStatusCodeException.class)
                .hasMessage("Invalid status code 201. \"value\"");
        verify(httpResponse, times(1)).close();
    }

    @Test
    public void testStreamResponse() throws Exception {
        CloseableHttpResponse httpResponse = this.mockResponse("\"value\"", 201, ContentType.TEXT_PLAIN.getMimeType());
        StringBuilder stringBuilder = new StringBuilder();
        OphHttpResponse<String> ophHttpResponse = new OphHttpResponseImpl<>(httpResponse);
        ophHttpResponse.expectedStatus(201)
                .consumeStreamWith(stream -> {
                    try {
                        while(stream.available() > 0) {
                            stringBuilder.append((char)stream.read());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        assertThat(stringBuilder.toString()).isEqualTo("\"value\"");
        verify(httpResponse, times(1)).close();
    }

    @Getter
    @Setter
    static class TestObject {
        String value;
        LocalDate localDate;
        LocalDateTime localDateTime;
        Date javaDate;
        java.sql.Date sqlDate;
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
