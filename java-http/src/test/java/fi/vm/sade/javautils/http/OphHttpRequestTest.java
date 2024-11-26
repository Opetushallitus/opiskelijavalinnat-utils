package fi.vm.sade.javautils.http;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;

public class OphHttpRequestTest {
    @Test
    public void responseJsonMappingTest() throws Exception {
        TestObject testObject = new TestObject();
        Gson gson = new Gson();
        // To make sure encodings don't break anything
        testObject.setTestField("test value with special characters öäå-~?!ÖÄÅ");

        String jsonContent = gson.toJson(testObject);
        OphHttpEntity entity = new OphHttpEntity.Builder()
                .content(jsonContent)
                .contentType("application/json", "UTF-8")
                .build();

        OphHttpRequest request = new OphHttpRequest.Builder("PUT", "http://test.fi/api")
                .setEntity(entity)
                .addHeader("Header1", "Header1_value")
                .build();
        HttpUriRequest uriRequest = request.getHttpUriRequest();
        TestObject handledObject = gson.fromJson(
            new InputStreamReader(((HttpEntityEnclosingRequestBase)uriRequest).getEntity().getContent()),
            TestObject.class);

        assertEquals("Object content didn't change on handling", handledObject.getTestField(), testObject.getTestField());
    }

    @Getter
    @Setter
    static class TestObject {
        String testField;
    }
}
