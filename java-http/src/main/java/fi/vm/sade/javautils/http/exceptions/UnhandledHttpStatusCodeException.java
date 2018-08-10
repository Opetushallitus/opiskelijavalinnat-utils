package fi.vm.sade.javautils.http.exceptions;

public class UnhandledHttpStatusCodeException extends RuntimeException {
    public UnhandledHttpStatusCodeException() {
        super();
    }

    public UnhandledHttpStatusCodeException(String message) {
        super(message);
    }

    public UnhandledHttpStatusCodeException(Throwable cause) {
        super(cause);
    }

    public UnhandledHttpStatusCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
