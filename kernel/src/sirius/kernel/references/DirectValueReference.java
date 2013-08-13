/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Simple implementation of {@link ValueReference} which always returns the
 * given object.
 */
public class DirectValueReference<E, V> implements ValueReference<E, V> {

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(E data) {
        return (V) data;
    }

    @Override
    public void setValue(E data, V value) {
    }

}
