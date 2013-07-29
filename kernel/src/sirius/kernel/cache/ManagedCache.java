package sirius.kernel.cache;

import com.google.common.cache.CacheBuilder;
import sirius.kernel.health.Counter;
import sirius.kernel.health.Exceptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

class ManagedCache<K, V> implements Cache<K, V> {

    private static final int MAX_HISTORY = 25;
    protected int maxSize;
    protected ValueComputer<K, V> computer;
    protected com.google.common.cache.Cache<K, CacheEntry<K, V>> data;
    protected Counter hits = new Counter();
    protected Counter misses = new Counter();
    protected List<Long> usesHistory = new ArrayList<Long>(MAX_HISTORY);
    protected List<Long> hitRateHistory = new ArrayList<Long>(MAX_HISTORY);
    protected Date lastEvictionRun = null;
    protected final String name;
    protected final long timeToLive;
    protected final ValueVerifier<V> verifier;
    private final long verificationInterval;

    public ManagedCache(String name,
                        int maxSize,
                        long timeToLive,
                        ValueComputer<K, V> valueComputer,
                        ValueVerifier<V> verifier,
                        long verificationInterval) {
        this.name = name;
        this.verificationInterval = verificationInterval;
        this.timeToLive = timeToLive;
        this.computer = valueComputer;
        this.verifier = verifier;
        ensureSize(maxSize);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int getSize() {
        return (int) data.size();
    }

    @Override
    public long getUses() {
        return hits.getCount() + misses.getCount();
    }

    @Override
    public Long getHitRate() {
        long h = hits.getCount();
        long m = misses.getCount();
        return h + m == 0L ? 0L : Math.round(100d * (double) h / (double) (h + m));
    }

    @Override
    public Date getLastEvictionRun() {
        return lastEvictionRun;
    }

    @Override
    public void runEviction() {
        usesHistory.add(getUses());
        if (usesHistory.size() > MAX_HISTORY) {
            usesHistory.remove(0);
        }
        hitRateHistory.add(getHitRate());
        if (hitRateHistory.size() > MAX_HISTORY) {
            hitRateHistory.remove(0);
        }
        hits.reset();
        misses.reset();
        lastEvictionRun = new Date();
        if (timeToLive <= 0) {
            return;
        }
        // Remove all outdated entries...
        long now = System.currentTimeMillis();
        int numEvicted = 0;
        Iterator<Entry<K, CacheEntry<K, V>>> iter = data.asMap().entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, CacheEntry<K, V>> next = iter.next();
            if (next.getValue().getMaxAge() > now) {
                iter.remove();
                numEvicted++;
            }
        }
        if (numEvicted > 0 && CacheManager.LOG.isFINE()) {
            CacheManager.LOG.FINE("Evicted %d entries from %s", numEvicted, name);
        }
    }

    @Override
    public void clear() {
        data.asMap().clear();
        misses.reset();
        hits.reset();
        lastEvictionRun = new Date();
    }

    @Override
    public V get(K key) {
        return get(key, this.computer);
    }

    @Override
    public boolean contains(K key) {
        return data.asMap().containsKey(key);
    }

    ;

    @Override
    public V get(final K key, final ValueComputer<K, V> computer) {
        try {
            long now = System.currentTimeMillis();
            CacheEntry<K, V> entry = data.get(key, new Callable<CacheEntry<K, V>>() {
                @Override
                public CacheEntry<K, V> call() throws Exception {
                    misses.inc();
                    if (computer != null) {
                        V value = computer.compute(key);
                        return new CacheEntry<K, V>(key,
                                                    value,
                                                    timeToLive > 0 ? timeToLive + System.currentTimeMillis() : 0,
                                                    verificationInterval + System.currentTimeMillis());
                    }
                    return null;
                }
            });
            if (entry != null && entry.getMaxAge() > 0 && entry.getMaxAge() < now) {
                data.invalidate(key);
                entry = null;
            }
            if (verifier != null && entry != null && verificationInterval > 0 && entry.getNextVerification() < now) {
                if (!verifier.valid(entry.getValue())) {
                    entry = null;
                } else {
                    entry.setNextVerification(verificationInterval + now);
                }
            }
            if (entry != null) {
                hits.inc();
                entry.getHits().inc();
                return entry.getValue();
            } else {
                misses.inc();
                return null;
            }
        } catch (Throwable e) {
            throw Exceptions.handle(e);
        }
    }

    @Override
    public void put(K key, V value) {
        CacheEntry<K, V> cv = new CacheEntry<K, V>(key,
                                                   value,
                                                   timeToLive > 0 ? timeToLive + System.currentTimeMillis() : 0,
                                                   verificationInterval + System.currentTimeMillis());
        data.put(key, cv);
    }

    @Override
    public void remove(K key) {
        data.invalidate(key);
    }

    @Override
    public Iterator<K> keySet() {
        return data.asMap().keySet().iterator();
    }

    @Override
    public List<CacheEntry<K, V>> getContents() {
        return new ArrayList<CacheEntry<K, V>>(data.asMap().values());
    }

    @Override
    public List<Long> getUseHistory() {
        return usesHistory;
    }

    @Override
    public List<Long> getHitRateHistory() {
        return hitRateHistory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void ensureSize(int intValue) {
        if (data == null || getMaxSize() != intValue) {
            maxSize = intValue;
            if (maxSize > 0) {
                this.data = CacheBuilder.newBuilder().maximumSize(maxSize).build();
            } else {
                this.data = CacheBuilder.newBuilder().build();
            }
        }

    }

}
