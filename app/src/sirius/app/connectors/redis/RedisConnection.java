/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.connectors.redis;

import redis.clients.jedis.Jedis;
import sirius.kernel.commons.Watch;

/**
* Created by aha on 25.06.14.
*/
public class RedisConnection implements AutoCloseable {
    private Jedis client;
    private Watch watch;

    RedisConnection(Jedis client) {
        this.client = client;
        this.watch = Watch.start();
    }

    @Override
    public void close() throws Exception {
        RedisConnector.redisConnections.addValue(watch.elapsedMillis());
        watch.submitMicroTiming("redis", "txn");
    }

    public Jedis getClient() {
        return client;
    }
}
