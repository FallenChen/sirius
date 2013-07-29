package sirius.kernel.cache;

/**
 * Computes a value if it is not found in a cached.
 */
public interface ValueComputer<K, V> {
    /**
     * Computes the value for the given key.
     */
    V compute(K key);

}
