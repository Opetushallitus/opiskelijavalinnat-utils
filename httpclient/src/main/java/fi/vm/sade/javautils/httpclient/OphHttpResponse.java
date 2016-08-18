package fi.vm.sade.javautils.httpclient;

import java.io.InputStream;
import java.util.List;

public interface OphHttpResponse extends AutoCloseable {
    InputStream asInputStream();
    int getStatusCode();
    List<String> getHeaderValues(String contentType);
    List<String> getHeaderKeys();
    OphRequestParameters getRequestParameters();

    /**
     * For testing only. Use asInputStream() instead.
     * @return
     */
    String asText();
}
