package fi.vm.sade.javautils.http;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;

import java.io.IOException;

@Getter
@Slf4j
public class HttpException extends IOException {

    private int statusCode;
    private String statusMsg;
    private String errorContent;

    public HttpException(HttpResponse response, String message) {
        super(message);
        this.statusCode = response.getStatusLine().getStatusCode();
        this.statusMsg = response.getStatusLine().getReasonPhrase();
        try {
            if (response.getEntity() != null) {
                this.errorContent = IOUtils.toString(response.getEntity().getContent());
            } else {
                this.errorContent = "no content";
            }

        } catch (IOException e) {
            log.error("error reading errorContent: "+e, e);
        }
    }
}