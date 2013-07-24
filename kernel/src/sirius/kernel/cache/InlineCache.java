package sirius.kernel.cache;

import sirius.kernel.commons.ValueProvider;

/**
 * Caches simple values to prevent frequent re-computation. The cache will never
 * empty itself. If a real lookup-cache is required, use {@link Cache} and
 * {@link CacheManager}.
 */
public class InlineCache<E> {
    private E buffer;
    private long lastComputation;
    private long timeout;
    private ValueProvider<E> computer;

    protected InlineCache(ValueProvider<E> computer, long timeout) {
        this.computer = computer;
        this.timeout = timeout;
    }

    public E get() {
        if (System.currentTimeMillis() - lastComputation > timeout) {
            buffer = computer.get();
            lastComputation = System.currentTimeMillis();
        }
        return buffer;
    }

    /**
     * Clears the cache.
     */
    public void flush() {
        lastComputation = 0;
    }
}
