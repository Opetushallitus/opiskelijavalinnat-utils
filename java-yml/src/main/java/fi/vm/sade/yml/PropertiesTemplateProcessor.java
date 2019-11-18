package fi.vm.sade.yml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hubspot.jinjava.Jinjava;

import java.util.Map;

public class PropertiesTemplateProcessor {
    private static final Jinjava jinjava = new Jinjava();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public static String processTemplate(String template, String vars) {
        try {
            MapType mapType  = yamlMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
            Map<String, String> attributes = yamlMapper.readValue(vars, mapType);
            return jinjava.render(template, attributes);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
