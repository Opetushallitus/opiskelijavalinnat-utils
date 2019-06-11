package fi.vm.sade.suomifi.valtuudet;

public enum ValtuudetType {

    PERSON("hpa"),
    ORGANISATION("ypa"),
    ;

    public final String path;

    ValtuudetType(String path) {
        this.path = path;
    }

}
