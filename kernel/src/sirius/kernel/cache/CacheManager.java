package sirius.kernel.cache;

import sirius.kernel.commons.ValueProvider;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to all managed system caches.
 */
public class CacheManager {

    /**
     * Logged used by the caching system
     */
    protected static final Log LOG = Log.get("cached");

    private static final String EXTENSION_TYPE_CACHES = "caches";
    private static final String CONFIG_KEY_MAX_SIZE = "maxSize";
    private static final String CONFIG_KEY_TTL = "ttl";
    private static final String CONFIG_KEY_VERIFICATION = "verification";

    private static List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>();

    /**
     * Returns a list of all caches.
     */
    public static List<Cache<?, ?>> getCaches() {
        return caches;
    }

    /**
     * Creates a cached with the given parameters.
     */
    public static <K, V> Cache<K, V> createCache(String name,
                                                 ValueComputer<K, V> valueComputer,
                                                 ValueVerifier<V> verifier) {
        if (!name.matches("[a-z0-9\\-]+")) {
            LOG.WARN("Bad cache name detected: '%s'. Names should only consist of lowercase letters, digits or '-'");
        }
        Extension cacheInfo = Extensions.getExtension(EXTENSION_TYPE_CACHES, name);
        if (cacheInfo == null) {
            LOG.WARN("Cache %s does not exist! Using defaults...", name);
            cacheInfo = Extensions.getExtension(EXTENSION_TYPE_CACHES, Extensions.DEFAULT);
        }
        Cache<K, V> result = new ManagedCache<K, V>(name,
                                                    cacheInfo.get(CONFIG_KEY_MAX_SIZE).asInt(100),
                                                    cacheInfo.get(CONFIG_KEY_TTL).asLong(60 * 60 * 1000),
                                                    valueComputer,
                                                    verifier,
                                                    cacheInfo.get(CONFIG_KEY_VERIFICATION).asLong(60 * 60 * 1000));
        caches.add(result);
        return result;
    }

    /**
     * Creates a cached with the given parameters.
     */
    public static <K, V> Cache<K, V> createCache(String name) {
        return createCache(name, null, null);
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
