/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

/**
 * Visible for {@link sirius.kernel.di.ClassLoadAction} at start-time to make components visible
 * in the GlobalContext.
 */
public interface MutableGlobalContext extends GlobalContext {
    /**
     * Registers the given part for the given lookup interfaces.
     */
    void registerPart(Object part, Class<?>... implementedInterfaces);

    /**
     * Registers the given part for the given name and interfaces.
     */
    void registerPart(String uniqueName, Object part, Class<?>... implementedInterfaces);
}
