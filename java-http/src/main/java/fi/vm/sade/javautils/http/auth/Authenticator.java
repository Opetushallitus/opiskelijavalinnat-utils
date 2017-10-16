package fi.vm.sade.javautils.http.auth;

import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public interface Authenticator {

    Authenticator NONE = new Authenticator() {
        @Override
        public boolean authenticate(HttpRequestBase httpRequestBase) throws IOException {
            return false;
        }

        @Override
        public String getUrlPrefix() {
            return "";
        }
    };

    boolean authenticate(HttpRequestBase httpRequestBase) throws IOException;

    String getUrlPrefix();

}
