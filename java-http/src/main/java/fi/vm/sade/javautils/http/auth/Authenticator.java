package fi.vm.sade.javautils.http.auth;

import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public interface Authenticator {

    Authenticator NONE = new Authenticator() {
        @Override
        public void clearSession() {
        }

        @Override
        public boolean authenticate(HttpUriRequest request) {
            return false;
        }

        @Override
        public String getUrlPrefix() {
            return "";
        }


    };

    void clearSession();

    boolean authenticate(HttpUriRequest request);

    String getUrlPrefix();

}
