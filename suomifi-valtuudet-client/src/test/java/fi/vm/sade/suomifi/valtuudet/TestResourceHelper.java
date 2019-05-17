package fi.vm.sade.suomifi.valtuudet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestResourceHelper {

    private TestResourceHelper() {
    }

    public static String loadAsString(String resourceName) {
        return loadAsString(resourceName, StandardCharsets.UTF_8);
    }

    public static String loadAsString(String resourceName, Charset charset) {
        try {
            Path path = Paths.get(TestResourceHelper.class.getClassLoader().getResource(resourceName).toURI());
            return new String(Files.readAllBytes(path), charset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
