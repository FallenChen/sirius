package sirius.kernel.cache;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a cached which contains a key value mapping.
 */
public interface Cache<K, V> {

    /**
     * Returns the name of the cached.
     */
    String getName();

    /**
     * Returns the max size of the cached.
     */
    int getMaxSize();

    /**
     * Returns the number of entries in the cached.
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
     * Returns the cached-hitrate (in percent)
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
     * Clears up the complete cached.
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
     * Removes the given item from the cached.
     */
    void remove(K key);

    /**
     * Checks if there is a cached entry for the given key.
     */
    boolean contains(K key);

    /**
     * Provides access to the keys stored in this cached.
     */
    Iterator<K> keySet();

    /**
     * Provides access to the contents of this cached.
     */
    List<CacheEntry<K, V>> getContents();

    /**
     * Verifies that the cached has the given size.
     */
    void ensureSize(int intValue);
}
