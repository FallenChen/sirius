/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.security;

import sirius.web.controller.Message;

/**
 * Created by aha on 24.09.14.
 */
public class UserContextHelper {

    public static boolean expectNoMessages() {
        return UserContext.get().getMessages().isEmpty();
    }

    public static boolean expectNoErrorMessages() {
        for (Message msg : UserContext.get().getMessages()) {
            if (msg.getType() == Message.ERROR) {
                return false;
            }
        }
        return true;
    }

    public static boolean expectErrorMessage() {
        for (Message msg : UserContext.get().getMessages()) {
            if (msg.getType() == Message.ERROR) {
                return true;
            }
        }
        return false;
    }

    public static boolean expectErrorMessageContaining(String textPart) {
        for (Message msg : UserContext.get().getMessages()) {
            if (msg.getType() == Message.ERROR && msg.getMessage().contains(textPart)) {
                return true;
            }
        }
        return false;
    }

    public static boolean expectSuccessMessage() {
        for (Message msg : UserContext.get().getMessages()) {
            if (msg.getType() == Message.INFO) {
                return true;
            }
        }
        return false;
    }

    public static boolean expectSuccessMessageContaining(String textPart) {
        for (Message msg : UserContext.get().getMessages()) {
            if (msg.getType() == Message.INFO && msg.getMessage().contains(textPart)) {
                return true;
            }
        }
        return false;
    }

}
