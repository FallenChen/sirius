/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.di;

import java.util.Collection;

/**
 * Provides access to basic OCM functions.
 * <p/>
 * Each OCM implementation should provide a way for non-component classes to
 * access an instance of {@link sirius.kernel.di.GlobalContext} in order to interact with OCM.
 */
public interface GlobalContext {

    /**
     * Finds the previously registered part for the given service interface. If several parts where
     * registered for this interface, the first one is chosen. If no part was
     * registered, <code>null</code> is returned.
     */
    <P> P getPart(Class<P> clazz);

    /**
     * Retrieves a part of the requested type with the given well known name. IF
     * the part is not found, <code>null</code> is returned.
     */
    <P> P getPart(String uniqueName, Class<P> clazz);

    /**
     * Retrieves a part of the requested type with the given well known name. If
     * the part is not found, an exception is thrown.
     */
    <P> P findPart(String uniqueName, Class<P> clazz);

    /**
     * Returns all parts which are currently registered for the given service
     * interface. During installation however, not all services may be returned
     * since not all components are completely installed.
     */
    <P> Collection<P> getParts(Class<P> partInterface);

    /**
     * Returns a collection which always contains all available parts for the
     * given service interface which are present at the time of call.
     */
    <P> PartCollection<P> getPartCollection(Class<P> partInterface);

    /**
     * Reads the annotations of the given object and auto-fills annotated
     * fields.
     */
    <T> T wire(T object);

}