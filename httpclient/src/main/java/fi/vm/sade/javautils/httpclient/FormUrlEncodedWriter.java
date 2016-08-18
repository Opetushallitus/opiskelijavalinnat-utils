package fi.vm.sade.javautils.httpclient;

import fi.vm.sade.properties.UrlUtils;

import java.io.IOException;
import java.io.Writer;

public class FormUrlEncodedWriter {
    private Writer outstream;
    private boolean firstParam = true;

    public FormUrlEncodedWriter(Writer outstream) {
        this.outstream = outstream;
    }

    public FormUrlEncodedWriter param(String key, Object value) throws IOException {
        if(firstParam) {
            firstParam = false;
        } else {
            outstream.write("&");
        }
        outstream.write(UrlUtils.encode(key));
        outstream.write("=");
        outstream.write(UrlUtils.encode(value.toString()));
        return this;
    }
}
