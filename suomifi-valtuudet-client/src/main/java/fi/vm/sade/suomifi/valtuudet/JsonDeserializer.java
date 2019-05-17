package fi.vm.sade.suomifi.valtuudet;

import java.io.IOException;

@FunctionalInterface
public interface JsonDeserializer {

    <T> T deserialize(String json, Class<T> type) throws IOException;

}
