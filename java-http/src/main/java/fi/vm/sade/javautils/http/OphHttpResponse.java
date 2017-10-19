package fi.vm.sade.javautils.http;

import java.io.InputStream;
import java.util.List;

public interface OphHttpResponse extends AutoCloseable {

    InputStream asInputStream();

    int getStatusCode();

    List<String> getHeaderValues(String key);

    List<String> getHeaderKeys();

    String asText();

}
