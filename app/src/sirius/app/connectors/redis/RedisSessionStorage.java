/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.connectors.redis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sirius.kernel.commons.Value;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.web.http.session.ServerSession;
import sirius.web.http.session.SessionStorage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by aha on 25.06.14.
 */
@Register(name = "redis")
public class RedisSessionStorage implements SessionStorage {

    @ConfigValue("redis.sessionStore")
    private String redisServer;

    public class RedisSession implements ServerSession {

        private static final String PREFIX = "session-";

        private String id;
        private Map<String, Object> localCache = Maps.newHashMap();


        @Override
        public long getCreationTime() {
            try {
                try (RedisConnection c = RedisConnector.getClient(redisServer)) {
                    return Value.of(c.getClient().get(PREFIX + id + "-create")).asLong(0);
                }
            } catch (Exception e) {
                throw Exceptions.handle(RedisConnector.LOG, e);
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public long getLastAccessedTime() {
            try {
                try (RedisConnection c = RedisConnector.getClient(redisServer)) {
                    return Value.of(c.getClient().get(PREFIX + id + "-last-access")).asLong(0);
                }
            } catch (Exception e) {
                throw Exceptions.handle(RedisConnector.LOG, e);
            }
        }

        @Override
        public int getMaxInactiveInterval() {
            try {
                try (RedisConnection c = RedisConnector.getClient(redisServer)) {
                    return Value.of(c.getClient().get(PREFIX + id + "-max-interval")).asInt(0);
                }
            } catch (Exception e) {
                throw Exceptions.handle(RedisConnector.LOG, e);
            }
        }

        @Nonnull
        @Override
        public Value getValue(String key) {
            if (!localCache.containsKey(key)) {
                try {
                    try (RedisConnection c = RedisConnector.getClient(redisServer)) {
                        localCache.put(key, c.getClient().hget(PREFIX + id, key));
                    }
                } catch (Exception e) {
                    throw Exceptions.handle(e);
                }
            }
            return Value.of(localCache.get(key));
        }

        @Override
        public List<String> getKeys() {
            try {
                try (RedisConnection c = RedisConnector.getClient(redisServer)) {
                    return Lists.newArrayList(c.getClient().hkeys(PREFIX + id));
                }
            } catch (Exception e) {
                throw Exceptions.handle(e);
            }
        }

        @Override
        public boolean hasKey(String key) {
            return false;
        }

        @Override
        public void putValue(String key, Object data) {

        }

        @Override
        public void removeValue(String key) {

        }

        @Override
        public void invalidate() {

        }

        @Override
        public boolean isNew() {
            return false;
        }
    }

    @Override
    public Optional<ServerSession> getSession(String id) {
        return null;
    }

    @Override
    public ServerSession createSession() {
        return null;
    }

    @Override
    public Stream<String> getSessions() {
        return null;
    }

    @Override
    public int getNumberOfSessions() {
        return 0;
    }
}
