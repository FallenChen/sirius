/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.timer;

public interface EveryDay extends TimedTask {
    /**
     * If no config value is given, this will return the default hour, in which
     * the task will be executed.
     */
    int getDefaultHourOfExecution();

    /**
     * Returns the name of the config key, which stores the hour of execution.
     */
    String getConfigKeyName();
}
