/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

/**
 * Provides a value reference which accesses an index of a given array.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class IndexValueReference<E> extends ReadonlyValueReference<E[], E> {

    private int index;

    /**
     * Creates a value reference which reads the given index in a given array.
     *
     * @param index the index to read the value.
     */
    public IndexValueReference(int index) {
        super();
        this.index = index;
    }

    /**
     * Returns the index within a given array, which is accessed by this reference.
     *
     * @return the index which is accessed by this reference.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the index within a given array, which is accessed by this reference.
     *
     * @param index the index which is accessed by this reference.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns the n-th (index) value of the given array.
     *
     * @param data the object from which the value is read
     * @return the n-th (index-th) value of the given array or <tt>null</tt> if the index used by this reference
     *         is larger than the array
     */
    @Override
    public E getValue(E[] data) {
        if (data == null || index >= data.length) {
            return null;
        }
        return data[index];
    }

}
