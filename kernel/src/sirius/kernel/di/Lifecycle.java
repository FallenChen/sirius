/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

/**
 * Classes implementing this interface get notified once the framework is started or being shut down.
 * <p>
 * In contrast to {@link Initializable} this might be called a bit later, since the system is first initialized
 * and then started up. However, one can assume that all annotations have been processed and dependent parts
 * can be accessed.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface Lifecycle {
    /**
     * Invoked when the framework starts up.
     */
    void started();

    /**
     * Invoked when the framework shuts down.
     */
    void stopped();

    /**
     * Returns a short name for this lifecycle.
     *
     * @return the name of this component, used for log outputs.
     */
    String getName();
}
