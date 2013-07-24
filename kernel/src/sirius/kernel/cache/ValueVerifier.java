package sirius.kernel.cache;

/**
 * Checks if a given value is still valid.
 */
public interface ValueVerifier<V> {
    boolean valid(V value);
}
