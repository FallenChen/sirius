package sirius.kernel.references;

/**
 * Provides read/write access to a value of a given object.
 */
public interface ValueReference<E, V> {

    /**
     * Reads the value.
     */
    V getValue(E data);

    /**
     * Writes the value.
     */
    void setValue(E data, V value);

}