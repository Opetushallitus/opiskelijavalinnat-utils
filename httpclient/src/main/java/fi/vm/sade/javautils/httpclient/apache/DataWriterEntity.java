package fi.vm.sade.javautils.httpclient.apache;

import fi.vm.sade.javautils.httpclient.OphRequestPostWriter;
import org.apache.http.entity.AbstractHttpEntity;

import java.io.*;

class DataWriterEntity extends AbstractHttpEntity {
    private OphRequestPostWriter dataWriter;
    private String charsetName;

    DataWriterEntity(String charsetName, OphRequestPostWriter dataWriter) {
        this.dataWriter = dataWriter;
        this.charsetName = charsetName;
    }

    public boolean isRepeatable() {
        return false;
    }

    public long getContentLength() {
        return -1;
    }

    public boolean isStreaming() {
        return false;
    }

    public InputStream getContent() throws IOException {
        // Should be implemented as well but is irrelevant for this case
        throw new UnsupportedOperationException();
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(outstream, charsetName), 128*1024);
        dataWriter.writeTo(writer);
        writer.flush();
    }
}