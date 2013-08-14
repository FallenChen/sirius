package sirius.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import sirius.kernel.async.CallContext;
import sirius.kernel.health.Log;

import java.util.List;
import java.util.Set;

public class UserContext {

    public static Log LOG = Log.get("user");
    private List<Message> msgList = Lists.newArrayList();
    private Set<String> fieldErrors = Sets.newTreeSet();

    public void addMessage(Message msg) {
        msgList.add(msg);
    }

    public List<Message> getMessages() {
        return msgList;
    }

    public void addFieldError(String field) {
        fieldErrors.add(field);
    }

    public boolean hasError(String field) {
        return fieldErrors.contains(field);
    }

    public String signalFieldError(String field) {
        return hasError(field) ? "error" : "";
    }

    public static void handle(Throwable e) {
        message(Message.error(e));
    }

    public static void message(Message msg) {
        CallContext.getCurrent().get(UserContext.class).addMessage(msg);
    }


    public static void setFieldError(String field) {
        CallContext.getCurrent().get(UserContext.class).addFieldError(field);
    }
}
