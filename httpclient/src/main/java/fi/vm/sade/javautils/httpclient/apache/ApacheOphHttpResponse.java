package fi.vm.sade.javautils.httpclient.apache;

import fi.vm.sade.javautils.httpclient.OphHttpResponse;
import fi.vm.sade.javautils.httpclient.OphRequestParameters;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class ApacheOphHttpResponse implements OphHttpResponse {
    private OphRequestParameters requestParameters;
    private HttpResponse response;

    ApacheOphHttpResponse(OphRequestParameters requestParameters, HttpResponse response) {
        this.requestParameters = requestParameters;
        this.response = response;
    }

    @Override
    public InputStream asInputStream() {
        try {
            return response.getEntity().getContent();
        } catch (IOException e) {
            throw new RuntimeException("Url: " + requestParameters.url, e);
        }
    }

    @Override
    public String asText() {
        try {
            return toString(asInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Url: " + requestParameters.url, e);
        }
    }

    @Override
    public void close() {
        try {
            ((CloseableHttpResponse)response).close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing connection: " + requestParameters.url, e);
        }
    }

    @Override
    public int getStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public List<String> getHeaderValues(String key) {
        List<String> ret = new ArrayList<>();
        for(Header h: response.getHeaders(key)) {
            ret.add(h.getValue());
        }
        return ret;
    }

    @Override
    public List<String> getHeaderKeys() {
        List<String> ret = new ArrayList<>();
        for(Header h: response.getAllHeaders()) {
            if(!ret.contains(h.getName())) {
                ret.add(h.getName());
            }
        }
        return ret;
    }

    @Override
    public OphRequestParameters getRequestParameters() {
        return requestParameters;
    }

    private static String toString(InputStream stream) throws IOException { // IO
        BufferedInputStream bis = new BufferedInputStream(stream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result;
        result = bis.read();
        while(result != -1) {
            buf.write((byte) result);
            result = bis.read();
        }
        return buf.toString();
    }
}
