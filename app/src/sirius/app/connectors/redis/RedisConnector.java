/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.connectors.redis;

import com.google.common.collect.Maps;
import org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.JedisPool;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Average;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.health.MetricProvider;
import sirius.web.health.MetricsCollector;

import java.util.Map;

/**
 * Created by aha on 25.06.14.
 */
public class RedisConnector {

    private static Map<String, JedisPool> connections = Maps.newConcurrentMap();

    public static final Log LOG = Log.get("redis");
    protected static Average redisConnections = new Average();

    @Register
    public static class RedisMetrics implements MetricProvider {

        @Override
        public void gather(MetricsCollector collector) {
            collector.metric("redis-connection-duration", "Redis-ConnectionDuration", redisConnections.getAndClearAverage(), "ms");
            collector.differentialMetric("redis-connections", "redis-connections", "Redis-Connections", redisConnections.getCount(), "/min");
        }
    }

    public static RedisConnection getClient(String name) {
        JedisPool pool = connections.get(name);
        if (pool == null) {
            Extension ext = Extensions.getExtension("connector.redis", name);
            if (ext.isDefault()) {
                throw Exceptions.handle().withSystemErrorMessage("Unknown redis connection: %s", name).to(LOG).handle();
            }
            GenericObjectPool.Config c = new GenericObjectPool.Config();
            c.maxActive = ext.get("maxActive").asInt(10);
            c.maxIdle = ext.get("maxIdle").asInt(1);
            pool = new JedisPool(c, ext.get("host").asString(), ext.get("port").asInt(0));
            connections.put(name, pool);
        }
        return new RedisConnection(pool.getResource());

    }
}
