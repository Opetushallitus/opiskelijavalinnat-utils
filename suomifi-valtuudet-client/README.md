# suomifi-valtuudet-client

## client construction (with jackson json deserializer)

    OphHttpClient httpClient = ApacheOphHttpClient.createDefaultOphClient("callerId", null);
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ValtuudetProperties properties = new ValtuudetPropertiesImpl("https://...", "clientId", "apiKey", "oauthPassword");
    ValtuudetClient client = new ValtuudetClientImpl(httpClient, objectMapper::readValue, properties);

## usage

see [ValtuudetClient](src/main/java/fi/vm/sade/suomifi/valtuudet/ValtuudetClient.java).
