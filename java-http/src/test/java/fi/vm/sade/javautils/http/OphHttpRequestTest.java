package fi.vm.sade.javautils.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OphHttpRequestTest {
    @Test
    public void responseJsonMappingTest() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TestObject testObject = new TestObject();
        // To make sure encodings don't break anything
        testObject.setTestField("test value with special characters öäå-~?!ÖÄÅ");

        String jsonContent = objectMapper.writerFor(TestObject.class).writeValueAsString(testObject);
        OphHttpEntity entity = new OphHttpEntity.Builder()
                .content(jsonContent)
                .contentType("application/json", "UTF-8")
                .build();

        OphHttpRequest request = new OphHttpRequest.Builder("PUT", "http://test.fi/api")
                .setEntity(entity)
                .addHeader("Header1", "Header1_value")
                .build();
        HttpUriRequest uriRequest = request.getHttpUriRequest();
        TestObject handledObject = objectMapper.readerFor(TestObject.class)
                .readValue(((HttpEntityEnclosingRequestBase)uriRequest).getEntity().getContent());

        assertEquals("Object content didn't change on handling", handledObject.getTestField(), testObject.getTestField());
    }

    @Getter
    @Setter
    static class TestObject {
        String testField;
    }
}
