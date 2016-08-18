package fi.vm.sade.javautils.httpclient;

import java.io.InputStream;
import java.util.List;

public interface OphHttpResponse {
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

    /**
     * For responses that have been opened with handleManually()
     * Don't use otherwise.
     */
    void close();

}
