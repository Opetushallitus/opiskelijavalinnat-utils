package fi.vm.sade.suomifi.valtuudet;

public class ValtuudetPropertiesImpl implements ValtuudetProperties {

    private String host;
    private String clientId;
    private String apiKey;
    private String oauthPassword;

    public ValtuudetPropertiesImpl() {
    }

    private ValtuudetPropertiesImpl(String host, String clientId, String apiKey, String oauthPassword) {
        this.host = host;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.oauthPassword = oauthPassword;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getOauthPassword() {
        return oauthPassword;
    }

    public void setOauthPassword(String oauthPassword) {
        this.oauthPassword = oauthPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String host;
        private String clientId;
        private String apiKey;
        private String oauthPassword;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder oauthPassword(String oauthPassword) {
            this.oauthPassword = oauthPassword;
            return this;
        }

        public ValtuudetProperties build() {
            return new ValtuudetPropertiesImpl(host, clientId, apiKey, oauthPassword);
        }

    }

}
