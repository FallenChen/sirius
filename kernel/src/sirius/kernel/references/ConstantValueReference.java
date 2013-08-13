/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Simple implementation of {@link ValueReference} where the value is always the internally stored value.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class ConstantValueReference<E, V> implements ValueReference<E, V> {

    private V value;

    /**
     * Creates a new reference with the given internal value.
     *
     * @param value the value which will be returned by <tt>getValue</tt>
     */
    public ConstantValueReference(V value) {
        this.value = value;
    }

    /**
     * Always returns the internally stored value (set in the constructor or via <tt>setValue</tt>).
     *
     * @param data ignored by this implementation
     * @return the internally stored value
     */
    @Override
    public V getValue(E data) {
        return value;
    }

    /**
     * Sets the internally stored value, regardless of the given data object.
     *
     * @param data  ignored by this implementation
     * @param value the value to be stored internally
     */
    @Override
    public void setValue(E data, V value) {
        this.value = value;
    }

}
