package fi.vm.sade.javautils.http;

import lombok.Getter;

@Getter
public class OphHttpEntity {

    private String content;
    private String contentType;

    private OphHttpEntity(Builder builder) {
        content = builder.content;
        contentType = builder.contentType;
    }

    public static final class Builder {
        private String content;
        private String contentType;

        public Builder() {
            content = "";
            contentType = "application/json";
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public OphHttpEntity build() {
            return new OphHttpEntity(this);
        }

    }

}
