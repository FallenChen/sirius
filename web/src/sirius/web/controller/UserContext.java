/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.async.CallContext;
import sirius.kernel.health.Log;
import sirius.kernel.nls.NLS;
import sirius.web.http.WebContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Can be used to collect messages and errors to be displayed to the user.
 * <p>
 * Get hold of a {@link UserContext} by calling <code>CallContext.getCurrent().get(UserContext.class)</code> or
 * by using a helper method like {@link #message(Message)}.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
public class UserContext {

    public static Log LOG = Log.get("user");
    private List<Message> msgList = Lists.newArrayList();
    private Map<String, String> fieldErrors = Maps.newHashMap();

    /**
     * Adds a message to be shown to the user.
     *
     * @param msg the message to be shown to the user
     */
    public void addMessage(Message msg) {
        msgList.add(msg);
    }

    /**
     * Returns all messages to be shown to the user.
     *
     * @return a list of messages to be shown to the user
     */
    public List<Message> getMessages() {
        return msgList;
    }

    /**
     * Adds an error for a given field
     *
     * @param field the name of the field
     * @param value the value which was supplied and rejected
     */
    public void addFieldError(String field, String value) {
        fieldErrors.put(field, value);
    }

    /**
     * Determines if there is an error for the given field
     *
     * @param field the field to check for errors
     * @return <tt>true</tt> if an error was added for the field, <tt>false</tt> otherwise
     */
    public boolean hasError(String field) {
        return fieldErrors.containsKey(field);
    }

    /**
     * Returns "error" if an error was added for the given field.
     *
     * @param field the field to check
     * @return "error" if an error was added for the given field, <tt>false</tt> otherwise
     */
    public String signalFieldError(String field) {
        return hasError(field) ? "error" : "";
    }

    /**
     * Returns the originally submitted field value even if it was rejected due to an error.
     *
     * @param field the name of the form field
     * @param value the entity value (used if no error occurred)
     * @return the originally submitted value (if an error occurred), the given value otherwise
     */
    public String getFieldValue(String field, Object value) {
        if (fieldErrors.containsKey(field)) {
            return fieldErrors.get(field);
        }
        return NLS.toUserString(value);
    }


    /**
     * Returns the originally submitted field value even if it was rejected due to an error.
     *
     * @param field the name of the form field
     * @return the originally submitted value (if an error occurred) or the parameter (using field as name)
     *         from the current {@link WebContext} otherwise
     */
    public String getFieldValue(String field) {
        if (fieldErrors.containsKey(field)) {
            return fieldErrors.get(field);
        }
        return CallContext.getCurrent().get(WebContext.class).get(field).getString();
    }

    /**
     * Returns all values submitted for the given field
     *
     * @param field the name of the field which values should be extracted
     * @return a list of values submitted for the given field
     */
    public Collection<String> getFieldValues(String field) {
        return CallContext.getCurrent().get(WebContext.class).getParameters(field);
    }

    /**
     * Handles the given exception by passing it to {@link sirius.kernel.health.Exceptions} and by creating an
     * appropriate message for the user.
     *
     * @param e the exception to handle
     */
    public static void handle(Throwable e) {
        message(Message.error(e));
    }

    /**
     * Adds a message to the current UserContext.
     *
     * @param msg the message to add
     */
    public static void message(Message msg) {
        CallContext.getCurrent().get(UserContext.class).addMessage(msg);
    }

    /**
     * Adds a field error to the current UserContext.
     *
     * @param field the field for which an error occurred
     * @param value the value which was rejected
     */
    public static void setFieldError(String field, Object value) {
        CallContext.getCurrent().get(UserContext.class).addFieldError(field, NLS.toUserString(value));
    }
}
