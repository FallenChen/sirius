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

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 21:51
 * To change this template use File | Settings | File Templates.
 */
public class ServerSession {

    public static final String INITIAL_URI = "_INITIAL_URI";
    public static final String USER_AGENT = "_USER_AGENT";
    public static final String USER = "_USER";

    public interface Listener {

        void sessionCreated(ServerSession session) throws Exception;

        void sessionInvalidated(ServerSession session) throws Exception;
    }

    @Parts(Listener.class)
    private static PartCollection<Listener> listeners;

    private static Map<String, ServerSession> sessions = Collections.synchronizedMap(new HashMap<String, ServerSession>());

    public static ServerSession getSession(String id) {
        ServerSession result = Strings.isEmpty(id) ? null : sessions.get(id);
        if (result != null) {
            result.access();
            return result;
        } else {
            return create();
        }
    }

    public static List<ServerSession> getSessions() {
        return new ArrayList<ServerSession>(sessions.values());
    }

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


    public long getCreationTime() {
        return created;
    }

    public String getId() {
        return id;
    }

    public long getLastAccessedTime() {
        return lastAccessed;
    }

    public int getMaxInactiveInterval() {
        if (numAccesses < 2) {
            return (int) miniSessionLifetime.getStandardSeconds();
        } else {
            return (int) sessionLifetime.getStandardSeconds();
        }
    }

    public Value getValue(String s) {
        return Value.of(values.get(s));
    }

    public Map<String, Object> getValueMap() {
        return values;
    }

    public void putValue(String s, Object o) {
        values.put(s, o);
    }

    public void removeValue(String s) {
        values.remove(s);
    }

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

    public boolean isNew() {
        return numAccesses == 0;
    }

    private void access() {
        numAccesses++;
        lastAccessed = System.currentTimeMillis();
    }

}
