package fi.vm.sade.javautils.httpclient;

import java.io.InputStream;
import java.util.List;

public interface OphHttpResponse {
    InputStream asInputStream();
    void close();
    int getStatusCode();
    List<String> getHeaders(String contentType);

    /**
     * Use asInputStream() if possible. For testing only.
     * @return
     */
    String asText();
}
