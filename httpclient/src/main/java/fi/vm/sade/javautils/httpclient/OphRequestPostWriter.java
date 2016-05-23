package fi.vm.sade.javautils.httpclient;

import java.io.IOException;
import java.io.Writer;

public interface OphRequestPostWriter {
    void writeTo(final Writer outstream) throws IOException;
}
