package sirius.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.async.CallContext;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;

import java.util.List;
import java.util.Map;

public class UserContext {

    public static Log LOG = Log.get("user");
    private List<Message> msgList = Lists.newArrayList();
    private Map<String, String> fieldErrors = Maps.newHashMap();

    public void addMessage(Message msg) {
        msgList.add(msg);
    }

    public List<Message> getMessages() {
        return msgList;
    }

    public void addFieldError(String field, String value) {
        fieldErrors.put(field, value);
    }

    public boolean hasError(String field) {
        return fieldErrors.containsKey(field);
    }

    public String signalFieldError(String field) {
        return hasError(field) ? "error" : "";
    }

    public String getFieldValue(String field, Object value) {
        if (fieldErrors.containsKey(field)) {
            return fieldErrors.get(field);
        }
        return NLS.toUserString(value);
    }

    public static void handle(Throwable e) {
        message(Message.error(e));
    }

    public static void message(Message msg) {
        CallContext.getCurrent().get(UserContext.class).addMessage(msg);
    }


    public static void setFieldError(String field, Object value) {
        CallContext.getCurrent().get(UserContext.class).addFieldError(field, NLS.toUserString(value));
    }
}
