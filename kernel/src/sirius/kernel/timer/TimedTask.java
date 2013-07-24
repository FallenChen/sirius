/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

public interface TimedTask {

    /**
     * Called every time the timer interval is fired.
     */
    void runTimer() throws Exception;

}