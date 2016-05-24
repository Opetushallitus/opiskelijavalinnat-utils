package fi.vm.sade.javautils.httpclient;

import java.io.IOException;

public interface OphHttpResponseHandler<R> {
    R handleResponse(OphHttpResponse response) throws IOException;
}
