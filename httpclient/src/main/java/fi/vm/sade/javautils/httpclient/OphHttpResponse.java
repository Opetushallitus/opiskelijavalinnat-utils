package fi.vm.sade.javautils.httpclient;

import java.io.InputStream;
import java.util.List;

public interface OphHttpResponse {
    InputStream asInputStream();
    String asText();
    void close();
    int getStatusCode();

    List<String> getHeaders(String contentType);
}
