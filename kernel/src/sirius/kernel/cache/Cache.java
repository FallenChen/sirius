package sirius.kernel.cache;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a cache which contains a key value mapping.
 */
public interface Cache<K, V> {

    /**
     * Returns the name of the cache.
     */
    String getName();

    /**
     * Returns the max size of the cache.
     */
    int getMaxSize();

    /**
     * Returns the number of entries in the cache.
     */
    int getSize();

    /**
     * Returns the number of reads since the last eviction.
     */
    long getUses();

    /**
     * Returns the statistical values of "uses" for the last some eviction
     * intervals.
     */
    List<Long> getUseHistory();

    /**
     * Returns the cache-hitrate (in percent)
     */
    Long getHitRate();

    /**
     * Returns the statistical values of "hit rate" for the last some eviction
     * intervals.
     */
    List<Long> getHitRateHistory();

    /**
     * Returns the date of the last eviction run.
     */
    Date getLastEvictionRun();

    /**
     * Executes the eviction strategy.
     */
    void runEviction();

    /**
     * Clears up the complete cache.
     */
    void clear();

    /**
     * Returns the value associated with the given key.
     */
    V get(K key);

    /**
     * Returns the value associated with the given key. If the value is not
     * found, the {@link ValueComputer} is invoked.
     */
    V get(K key, ValueComputer<K, V> computer);

    /**
     * Stores the given key value mapping.
     */
    void put(K key, V value);

    /**
     * Removes the given item from the cache.
     */
    void remove(K key);

    /**
     * Checks if there is a cache entry for the given key.
     */
    boolean contains(K key);

    /**
     * Provides access to the keys stored in this cache.
     */
    Iterator<K> keySet();

    /**
     * Provides access to the contents of this cache.
     */
    List<CacheEntry<K, V>> getContents();

    /**
     * Verifies that the cache has the given size.
     */
    void ensureSize(int intValue);
}
