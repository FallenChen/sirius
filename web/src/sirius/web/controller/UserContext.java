package sirius.web.controller;

import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;

import java.util.ArrayList;
import java.util.List;

public class UserContext {

    public static Log LOG = Log.get("user");
    private List<Message> msgList = new ArrayList<Message>();

    public static class Message {

        public static final String INFO = "alert-info";
        public static final String WARN = "alert-warn";
        public static final String ERROR = "alert-error";

        private String message;
        private String details;
        private String type;

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
            return new Message(Exceptions.handle(LOG, t).getMessage(), null, ERROR);
        }
    }

    public void addMessage(Message msg) {
        msgList.add(msg);
    }

    public List<Message> getMessages() {
        return msgList;
    }

    public boolean isLoggedIn() {
        return true;
    }

    public String userName() {
        return "Andy";
    }

}
