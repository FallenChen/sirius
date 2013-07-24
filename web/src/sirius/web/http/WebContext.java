package sirius.web.http;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Value;
import sirius.kernel.di.annotations.ConfigValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 14.07.13
 * Time: 20:39
 * To change this template use File | Settings | File Templates.
 */
public class WebContext {
    public static final String SERVER_SESSION_SOURCE_PARAMETER = "SERVER_SESSION_SOURCE_PARAMETER";
    public static final String SERVER_SESSION_SOURCE_COOKIE = "SERVER_SESSION_SOURCE_COOKIE";
    private ChannelHandlerContext ctx;
    private Map<String, String> session;
    private Map<String, Object> attribute;
    private HttpRequest request;
    private String requestedURI;
    private Map<String, List<String>> queryString;
    private Map<String, Cookie> cookies;
    private boolean sessionModified;

    private String serverSessionSource;
    private ServerSession serverSession;

    @ConfigValue("http.sessionCookieName")
    private static String sessionCookieName;

    @ConfigValue("http.serverSessionParameterName")
    private static String serverSessionParameterName;

    @ConfigValue("http.serverSessionCookieName")
    private static String serverSessionCookieName;


    public WebContext() {
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public Value getSession(String key) {
        if (session == null) {
            initSession();
        }
        return Value.of(session.get(key));
    }

    private void initSession() {
        session = Maps.newHashMap();
        String encodedSession = getCookieValue(sessionCookieName);
        if (Strings.isFilled(encodedSession)) {
            QueryStringDecoder qsd = new QueryStringDecoder(encodedSession);
            for (Map.Entry<String, List<String>> entry : qsd.getParameters().entrySet()) {
                session.put(entry.getKey(), Iterables.getFirst(entry.getValue(), null));
            }
        }
    }

    public void setSession(String key, String value) {
        if (session == null) {
            initSession();
        }
        session.put(key, value);
        sessionModified = true;
    }

    public ServerSession getServerSession(boolean create) {
        String sid = getParameter(serverSessionParameterName);
        serverSessionSource = SERVER_SESSION_SOURCE_PARAMETER;
        if (Strings.isEmpty(sid)) {
            serverSessionSource = SERVER_SESSION_SOURCE_COOKIE;
            sid = getCookieValue(serverSessionCookieName);
            if (Strings.isEmpty(sid)) {
                serverSessionSource = null;
            }
        }
        if (Strings.isFilled(sid) || create) {
            return ServerSession.getSession(sid);
        }

        return null;
    }

    public String getServerSessionSource() {
        if (serverSessionSource == null && serverSession == null) {
            getServerSession(false);
        }

        return serverSessionSource;
    }

    public ServerSession getServerSession() {
        return getServerSession(true);
    }

    public String getRequestedURI() {
        if (requestedURI == null) {
            decodeQueryString();
        }
        return requestedURI;
    }

    public String getParameter(String key) {
        return Iterables.getFirst(getParameters(key), null);
    }

    public List<String> getParameters(String key) {
        if (queryString == null) {
            decodeQueryString();
        }
        List<String> result = queryString.get(key);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    private void decodeQueryString() {
        QueryStringDecoder qsd = new QueryStringDecoder(request.getUri(), CharsetUtil.UTF_8);
        requestedURI = qsd.getPath();
        queryString = qsd.getParameters();
    }

    public Cookie getCookie(String name) {
        if (cookies == null) {
            cookies = Maps.newHashMap();
            CookieDecoder cd = new CookieDecoder();
            for (Cookie cookie : cd.decode(request.getHeader(HttpHeaders.Names.COOKIE))) {
                cookies.put(cookie.getName(), cookie);
            }
        }

        return cookies.get(name);
    }

    public String getCookieValue(String name) {
        Cookie c = getCookie(name);
        if (c == null) {
            return null;
        }
        return c.getValue();
    }


    public Response respond() {
        return new Response(this);
    }

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";

    public long getDateHeader(String header) {
        String value = request.getHeader(header);
        if (Strings.isEmpty(value)) {
            return 0;
        }
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            return dateFormatter.parse(value).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public Collection<String> getParameterNames() {
        if (queryString == null) {
            decodeQueryString();
        }
        return queryString.keySet();
    }

    public String getQueryString() {
        return request.getUri().substring(getRequestedURI().length());
    }
}
