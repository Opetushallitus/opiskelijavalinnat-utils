package fi.vm.sade.javautils.nio.cas.exceptions;

import org.asynchttpclient.Response;

public class MissingSessionCookieException extends Exception {
    private final String sessionName;
    private final Response response;

    public MissingSessionCookieException(String sessionName, Response response) {
        this.sessionName = sessionName;
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public String getSessionName() {
        return sessionName;
    }

    @Override
    public String toString() {
        return "MissingSessionCookieException{" +
                "sessionName='" + sessionName + '\'' +
                ", response=" + response +
                '}';
    }
}
