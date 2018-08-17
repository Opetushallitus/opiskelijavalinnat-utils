package fi.vm.sade.javautils.http.mappers;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.*;
import lombok.Getter;

import java.util.Date;

/**
 * Wrapper class for gson with required configurations. Gson is final class so it cannot be extended.
 */
@Getter
public class GsonConfiguration {
    private Gson gson;

    public GsonConfiguration() {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
                .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
                .registerTypeAdapter(java.sql.Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new java.sql.Date(json.getAsJsonPrimitive().getAsLong()))
                .registerTypeAdapter(java.sql.Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive((date).getTime()))
                ;
        this.gson = Converters.registerAll(gsonBuilder)
                .create();
    }

}
