package fi.vm.sade.javautils.nio.cas.exceptions;

public class TicketGrantingTicketException extends RuntimeException {

    public TicketGrantingTicketException(String msg) {
        super(msg);
    }

    public TicketGrantingTicketException(String msg, Throwable e) {
        super(msg, e);
    }
}
