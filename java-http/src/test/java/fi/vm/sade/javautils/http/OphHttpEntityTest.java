package fi.vm.sade.javautils.http;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OphHttpEntityTest {
    @Test
    public void createDefaultEntity() {
        OphHttpEntity entity = new OphHttpEntity.Builder().build();
        assertEquals("Default content is set", "", entity.getContent());
        assertEquals("Default mime type is set", "application/json", entity.getContentType().getMimeType());
        assertEquals("Default charset is set", "UTF-8", entity.getContentType().getCharset().displayName());
    }

    @Test
    public void createEntity() {
        String jsonContent = "{\"field\": \"value\"}";
        OphHttpEntity entity = new OphHttpEntity.Builder()
                .content(jsonContent)
                .contentType("application/json", "UTF-8")
                .build();
        assertEquals("Content is set", jsonContent, entity.getContent());
        assertEquals("Mime type is set", "application/json", entity.getContentType().getMimeType());
        assertEquals("Charset is set", "UTF-8", entity.getContentType().getCharset().displayName());
    }
}
