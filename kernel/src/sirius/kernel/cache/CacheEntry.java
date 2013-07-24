package sirius.kernel.cache;

import sirius.kernel.health.Counter;

import java.util.Date;

/**
 * Represents a cached entry.
 */
public class CacheEntry<K, V> {
    /**
     * Provides the number of hits.
     */
    protected Counter hits = new Counter();
    /**
     * Timestamp when the entry was added to the cache.
     */
    protected long created = 0;
    /**
     * Timestamp when the entry was last used.
     */
    protected long used = 0;
    /**
     * The key for this value
     */
    protected final K key;
    /**
     * The cached value.
     */
    protected V value;
    /**
     * Returns the max age of an entry.
     */
    protected long maxAge;
    protected long nextVerification;

    public Counter getHits() {
        return hits;
    }

    public long getUses() {
        return hits.getCount();
    }

    public Date getCreated() {
        return new Date(created);
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public Date getUsed() {
        return new Date(used);
    }

    public Date getTtl() {
        return new Date(maxAge);
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public CacheEntry(K key, V value, long maxAge, long nextVerification) {
        super();
        this.key = key;
        this.maxAge = maxAge;
        this.nextVerification = nextVerification;
        this.used = System.currentTimeMillis();
        this.created = used;
        this.value = value;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public long getNextVerification() {
        return nextVerification;
    }

    public void setNextVerification(long nextVerification) {
        this.nextVerification = nextVerification;
    }

    public K getKey() {
        return key;
    }

}
