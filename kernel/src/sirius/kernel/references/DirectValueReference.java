/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Simple implementation of {@link ValueReference} which always returns the given object as value.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class DirectValueReference<E, V> implements ValueReference<E, V> {

    /**
     * Directly returns the given data.
     *
     * @param data the object from which the value is read
     * @return the given object as result
     */
    @Override
    public V getValue(E data) {
        return (V) data;
    }

    /**
     * This methods hasn't any effect in this implementation
     *
     * @param data  is ignored
     * @param value is ignored
     */
    @Override
    public void setValue(E data, V value) {
    }

}
