/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

import javax.annotation.Nullable;

/**
 * Provides read and write access to a value based on an object reference.
 * <p>
 * Provides a value, like a property which can be read from and written to when giving a concrete object.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface ValueReference<E, V> {

    /**
     * Reads the value from the given object.
     *
     * @param data the object from which the value is read
     * @return the value read from the given object
     */
    @Nullable
    V getValue(@Nullable E data);

    /**
     * Writes the value to the given object.
     *
     * @param data  the object to be filled
     * @param value the value to be written into the object
     */
    @Nullable
    void setValue(@Nullable E data, @Nullable V value);

}