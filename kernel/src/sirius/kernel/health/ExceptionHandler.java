/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.health;

import sirius.kernel.commons.Tuple;

import java.util.List;

/**
 * Instances registered for this interface will be notified about every exception handled by {@link Exceptions}
 */
public interface ExceptionHandler {
    /**
     * Invoked to handle the given exception.
     * <p>
     * Can be used to get notified about any exception which occurs in the system.
     * </p>
     *
     * @param category  the category (logger name) used to handle this exception.
     * @param location  a string containing the source location where the exception occurred. This can be used to
     *                  filter equal exceptions and to simply increment a counter. This value might be null, if no
     *                  stacktrace was available.
     * @param mdc       the mapped diagnostic context for the current thread
     * @param exception the generated exception which will be sent to the user. If a exception was passed in, this
     *                  will be used as cause of this exception.
     * @throws Exception as this method is already called from within the exception handling system, errors in here
     *                   should not be sent there again, but simply be thrown by this method
     * @see sirius.kernel.async.CallContext#getMDC()
     * @see sirius.kernel.health.Exceptions#handle()
     */
    void handle(String category,
                String location,
                List<Tuple<String, String>> mdc,
                HandledException exception) throws Exception;
}
