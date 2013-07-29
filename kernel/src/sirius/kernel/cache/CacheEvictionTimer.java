package sirius.kernel.cache;

import sirius.kernel.di.annotations.Register;
import sirius.kernel.timer.EveryTenMinutes;

/**
 * Invoked regularly to remove outdated entries from the system caches...
 */
@Register
class CacheEvictionTimer implements EveryTenMinutes {

    @Override
    public void runTimer() throws Exception {
        for (Cache<?, ?> cache : CacheManager.getCaches()) {
            CacheManager.LOG.FINE("Running cached eviction for: %s", cache.getName());
            cache.runEviction();
        }

    }

}
