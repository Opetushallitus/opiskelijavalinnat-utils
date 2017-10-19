package fi.vm.sade.javautils.http;

import com.google.gson.*;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import lombok.extern.slf4j.Slf4j;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

@Slf4j
public class JsonParser {

    private Gson gson;

    private static ThreadLocal<DateFormat> df1 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm"));
    private static ThreadLocal<DateFormat> df2 = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    JsonParser() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(XMLGregorianCalendar.class, (JsonDeserializer<XMLGregorianCalendar>) (json, typeOfT, context) -> {
            String string = json.getAsString();
            try {
                return parseXmlGregorianCalendar(string);
            } catch (Throwable t){
                return null;
            }
        });
        gsonBuilder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()));
        gson = gsonBuilder.create();
    }

    public <T> T fromJson(Class<? extends T> resultType, String response) throws IOException {
        try {
            return gson.fromJson(response, resultType);
        } catch (JsonSyntaxException e) {
            throw new IOException("Failed to parse object from (json) response, type: " + resultType.getSimpleName()
                    + ", reason: " + e.getCause() + ", response:\n" + response);
        }
    }

    private static XMLGregorianCalendar parseXmlGregorianCalendar(String string) {
        if (string == null || string.isEmpty()) {
            log.debug("Error parsing json to XMLGregorianCalendar. String was null or empty!");
            return null;
        }

        final boolean hasSemicolon = string.contains(":");
        final boolean hasDash = string.contains("-");

        try {
            GregorianCalendar cal = new GregorianCalendar();
            if (hasSemicolon) {
                cal.setTime(df1.get().parse(string));
            } else if (hasDash) {
                cal.setTime(df2.get().parse(string));
            } else {
                cal.setTime(new Date(Long.parseLong(string)));
            }
            return new XMLGregorianCalendarImpl(cal);
        } catch (Throwable th) {
            log.warn("Error parsing json to XMLGregorianCalendar: " + string);
        }
        return null;
    }

}
