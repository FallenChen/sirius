package sirius.web.controller;

import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;

/**
* Created with IntelliJ IDEA.
*
* @author Andreas Haufler (aha@scireum.de)
* @since 2013/08
*/
public class Message {

    public static final String INFO = "alert-info";
    public static final String WARN = "alert-warn";
    public static final String ERROR = "alert-error";

    private String message;
    private String details;
    private String type;
    private String action;

    public Message(String message, String details, String type) {
        this.message = message;
        this.details = details;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public String getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public Message withAction(String link) {
        this.action = link;
        return this;
    }

    public static Message info(String message) {
        return new Message(message, null, INFO);
    }

    public static Message warn(String message) {
        return new Message(message, null, WARN);
    }

    public static Message error(String message) {
        return new Message(message, null, ERROR);
    }

    public static Message error(Throwable t) {
        if (t instanceof HandledException) {
            return error(t.getMessage());
        }
        return new Message(Exceptions.handle(UserContext.LOG, t).getMessage(), null, ERROR);
    }
}
