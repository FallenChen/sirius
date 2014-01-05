/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.google.common.collect.Maps;
import org.joda.time.Duration;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.di.PartCollection;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.timer.EveryMinute;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents a server sided session.
 * <p>
 * A ServerSession is identified by a UUID and kept on the server until a usage timeout occurs. The timeout depends on
 * the state of the session. The initial timeout defaults to 5 minutes and can be set via
 * <tt>http.serverMiniSessionLifetime</tt>. After the second use of the session, it will be set to 30 min (or the
 * value defined in <tt>http.serverSessionLifetime</tt>. This permits to get rid of useless "one-calL" sessions created
 * by bots like Google etc.
 * </p>
 * <p>
 * Normally the WebContext takes care of finding or creating sessions based on cookies.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @see sirius.web.http.WebContext#getServerSession()
 * @since 2013/08
 */
public class ServerSession {

    /**
     * Fixed field used to store the initial URI used to create this session
     */
    public static final String INITIAL_URI = "_INITIAL_URI";

    /**
     * Fixed field containing the user agent used to request the initial url
     */
    public static final String USER_AGENT = "_USER_AGENT";

    /**
     * Fixes field storing the name of the current user owning this session
     */
    public static final String USER = "_USER";

    /**
     * Listeners can be registered to be notified when a session was created or invalidated.
     */
    public interface Listener {

        /**
         * Notifies the listener, that a new session was created.
         *
         * @param session the newly created session
         * @throws Exception all exceptions get auto handled and will be logged
         */
        void sessionCreated(ServerSession session) throws Exception;

        /**
         * Notifies the listener, that a new session was invalidated.
         *
         * @param session the invalidated session
         * @throws Exception all exceptions get auto handled and will be logged
         */
        void sessionInvalidated(ServerSession session) throws Exception;
    }

    @Parts(Listener.class)
    private static PartCollection<Listener> listeners;

    private static Map<String, ServerSession> sessions = Collections.synchronizedMap(new HashMap<String, ServerSession>());

    /**
     * Returns the session for the given id. Creates a new session, if currently non-existed.
     *
     * @param id session id of the requested session
     * @return the ServerSession associated with the given id
     */
    public static ServerSession getSession(String id) {
        ServerSession result = Strings.isEmpty(id) ? null : sessions.get(id);
        if (result != null) {
            result.access();
            return result;
        } else {
            return create();
        }
    }

    /**
     * Returns a list of all available sessions
     *
     * @return the list of all active server sessions
     */
    public static List<ServerSession> getSessions() {
        return new ArrayList<ServerSession>(sessions.values());
    }

    /**
     * Creates a new session.
     *
     * @return the newly created session
     */
    public static ServerSession create() {
        ServerSession s = new ServerSession();
        sessions.put(s.getId(), s);

        for (Listener l : listeners.getParts()) {
            try {
                l.sessionCreated(s);
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
        }

        return s;
    }

    /**
     * Used to invalidate outdated sessions.
     * <p>
     * A session is outdated if the last access time is older than the specified timeout
     * </p>
     */
    @Register
    public static class SessionManager implements EveryMinute {

        @Override
        public void runTimer() throws Exception {
            for (ServerSession s : getSessions()) {
                if (System.currentTimeMillis() - s.getLastAccessedTime() > TimeUnit.MILLISECONDS
                        .convert(s.getMaxInactiveInterval(),
                                TimeUnit.SECONDS)) {
                    s.invalidate();
                }
            }
        }
    }


    private long created = System.currentTimeMillis();
    private Map<String, String> createdMDC;
    private Map<String, Object> values = Maps.newHashMap();
    private long lastAccessed = System.currentTimeMillis();
    private int numAccesses = 0;
    private String id = UUID.randomUUID().toString();

    @ConfigValue("http.serverMiniSessionLifetime")
    private static Duration miniSessionLifetime;

    @ConfigValue("http.serverSessionLifetime")
    private static Duration sessionLifetime;


    /**
     * Returns the timestamp of the sessions creation
     *
     * @return the timestamp in milliseconds when the session was created
     */
    public long getCreationTime() {
        return created;
    }

    /**
     * Returns the unique session id
     *
     * @return the session id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the timestamp when the session was last accessed
     *
     * @return the timestamp in milliseconds when the session was last accessed
     */
    public long getLastAccessedTime() {
        return lastAccessed;
    }

    /**
     * Returns the max. time span a session is permitted to be inactive (not accessed) before it is eligible for
     * invalidation.
     * <p>
     * If the session was not accessed since its creation, this time span is rather short, to get quickly rid of
     * "one call" sessions created by bots. After the second call, the timeout is expanded.
     * </p>
     *
     * @return the max number of seconds since the last access before the session is eligible for invalidation
     */
    public int getMaxInactiveInterval() {
        if (numAccesses < 2) {
            return (int) miniSessionLifetime.getStandardSeconds();
        } else {
            return (int) sessionLifetime.getStandardSeconds();
        }
    }

    /**
     * Fetches a previously stored value from the session.
     *
     * @param key the key for which the value is requested.
     * @return the stored value, wrapped as {@link Value}
     */
    @Nonnull
    public Value getValue(String key) {
        return Value.of(values.get(key));
    }

    /**
     * Directly returns the bare values map.
     * <p>
     * As this directly returns the internally used Map, the returned value should be handled with care.
     * </p>
     *
     * @return the stored values as Map.
     */
    public Map<String, Object> getValueMap() {
        return values;
    }

    /**
     * Stores the given name value pair in the session.
     *
     * @param key  the key used to associate the data with
     * @param data the data to store for the given key
     */
    public void putValue(String key, Object data) {
        values.put(key, data);
    }

    /**
     * Deletes the stored value for the given key.
     *
     * @param key the key identifying the data to be removed
     */
    public void removeValue(String key) {
        values.remove(key);
    }

    /**
     * Deletes this session.
     */
    public void invalidate() {
        for (Listener l : listeners.getParts()) {
            try {
                l.sessionCreated(this);
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
        }

        sessions.remove(getId());
    }

    /**
     * Determines if the session is new.
     * <p>
     * A new session was created by the current request and not fetched from the session map using its ID.
     * </p>
     *
     * @return <tt>true</tt> if the session was created by this request, <tt>false</tt> otherwise.
     */
    public boolean isNew() {
        return numAccesses == 0;
    }

    /*
     * Updates the last access values
     */
    private void access() {
        numAccesses++;
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return id + " (Created by: " + getValue(INITIAL_URI).asString() + ")";
    }
}
