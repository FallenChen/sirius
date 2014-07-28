/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

/**
 * Created by aha on 25.07.14.
 */
public interface TaskContextAdapter {
    void log(String apply);

    void trace(String apply);

    void setState(String apply);

    void inc(String counter, long duration);

    void markErroneous();

    void cancel();
}
