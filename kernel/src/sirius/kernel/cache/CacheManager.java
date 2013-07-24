package sirius.kernel.cache;

import sirius.kernel.commons.ValueProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to all managed system caches.
 */
public class CacheManager {
    private static List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>();

    /**
     * Returns a list of all caches.
     */
    public static List<Cache<?, ?>> getCaches() {
        return caches;
    }

    /**
     * Creates a cache with the given parameters.
     */
    public static <K, V> Cache<K, V> createCache(String name,
                                                 ValueComputer<K, V> valueComputer,
                                                 ValueVerifier<V> verifier) {
        Cache<K, V> result = new ManagedCache<K, V>(name,
                                                    maxSize,
                                                    TimeUnit.MILLISECONDS.convert(ttl, ttlUnit),
                                                    valueComputer,
                                                    verifier,
                                                    TimeUnit.MILLISECONDS
                                                            .convert(verificationInterval, verificationUnit));
        caches.add(result);
        return result;
    }

    /**
     * Creates a new {@link InlineCache} with the given TTL and computer.
     */
    public static <E> InlineCache<E> createInlineCache(long ttl, TimeUnit ttlUnit, ValueProvider<E> computer) {
        return new InlineCache<E>(computer, TimeUnit.MILLISECONDS.convert(ttl, ttlUnit));
    }

    /**
     * Creates a new {@link InlineCache} which waits 10 seconds until values are
     * re-computed.
     */
    public static <E> InlineCache<E> createTenSecondsInlineCache(ValueProvider<E> computer) {
        return new InlineCache<E>(computer, TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
    }
}
