package fi.vm.sade.yml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

public class PropertiesTemplateProcessorTest {
    private String template = readFile("/fi/vm/sade/yml/test.properties.template");
    private String vars = readFile("/fi/vm/sade/yml/test.properties.yml");
    private String templateWithDefaults = readFile("/fi/vm/sade/yml/testwithdefaults.properties.template");
    private String varsForDefaults = readFile("/fi/vm/sade/yml/testwithdefaults.properties.yml");

    @Test
    public void templateGetsValuesFromYml() {
        String populatedTemplate = PropertiesTemplateProcessor.processTemplate(template, vars);
        assertThat(populatedTemplate, containsString("value=abc"));
    }

    @Test
    public void defaultValuesCanBeSpecifiedInTemplate() {
        String populatedTemplate = PropertiesTemplateProcessor.processTemplate(templateWithDefaults, varsForDefaults);
        assertThat(populatedTemplate, containsString("value=abc"));
        assertThat(populatedTemplate, containsString("value_with.overridden.default=This is from YML"));
        assertThat(populatedTemplate, containsString("value_with.default.in.use=Hope you can see this."));
    }

    private String readFile(String classPathUrl) {
        try {
            return IOUtils.toString(getClass().getResource(classPathUrl), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
