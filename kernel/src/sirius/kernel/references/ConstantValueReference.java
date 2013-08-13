/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Simple implementation of {@link ValueReference} where the value is always the
 * same.
 */
public class ConstantValueReference<E, V> implements ValueReference<E, V> {

    private V value;

    public ConstantValueReference(V value) {
        this.value = value;
    }

    @Override
    public V getValue(E data) {
        return value;
    }

    @Override
    public void setValue(E data, V value) {
        this.value = value;
    }

}
