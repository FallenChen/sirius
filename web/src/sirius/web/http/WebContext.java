package sirius.web.http;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.multipart.*;
import org.jboss.netty.util.CharsetUtil;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Callback;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.commons.Value;
import sirius.kernel.di.annotations.ConfigValue;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.nls.NLS;
import sirius.kernel.xml.StructuredInput;
import sirius.kernel.xml.XMLStructuredInput;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
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

    public static enum ServerSessionSource {
        UNKNOWN, PARAMETER, COOKIE;
    }

    private ChannelHandlerContext ctx;
    private Map<String, Object> attribute;
    private HttpRequest request;
    private String requestedURI;
    private Map<String, List<String>> queryString;
    private Map<String, Cookie> cookiesIn;
    private Map<String, Cookie> cookiesOut;
    private HttpPostRequestDecoder postDecoder;
    private Attribute content;
    private Map<String, String> session;
    private boolean sessionModified;

    // Used by Response - but stored here, since a new Response might be created....
    protected boolean responseCommitted;
    protected Callback<CallContext> completionCallback;


    private ServerSessionSource serverSessionSource;
    private ServerSession serverSession;

    @ConfigValue("http.sessionCookieName")
    private static String sessionCookieName;

    @ConfigValue("http.sessionSecret")
    private static String sessionSecret;

    @ConfigValue("http.serverSessionParameterName")
    private static String serverSessionParameterName;

    @ConfigValue("http.serverSessionCookieName")
    private static String serverSessionCookieName;

    @ConfigValue("http.contextPrefix")
    private static String contextPrefix;

    @ConfigValue("http.maxStructuredInputSize")
    private static long maxStructuredInputSize;

    @ConfigValue("http.addP3PHeader")
    protected static boolean addP3PHeader;

    private static final ObjectMapper mapper = new ObjectMapper();


    public WebContext() {
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void onComplete(Callback<CallContext> onComplete) {
        completionCallback = onComplete;
    }

    protected void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public Value get(String key) {
        if (attribute != null && attribute.containsKey(key)) {
            return Value.of(attribute.get(key));
        }
        if (queryString.containsKey(key)) {
            List<String> val = getParameters(key);
            if (val.size() == 1) {
                return Value.of(val.get(0));
            } else if (val.size() == 0) {
                return Value.of(null);
            } else {
                return Value.of(val);
            }
        }
        if (postDecoder != null) {
            try {
                InterfaceHttpData data = postDecoder.getBodyHttpData(key);
                if (data != null && data instanceof Attribute) {
                    return Value.of(((Attribute) data).getValue());
                }
            } catch (Throwable e) {
                Exceptions.handle(WebServer.LOG, e);
            }
        }
        return Value.of(null);
    }

    public HttpData getHttpData(String key) {
        if (postDecoder == null) {
            return null;
        }
        try {
            InterfaceHttpData data = postDecoder.getBodyHttpData(key);
            if (data != null && data instanceof HttpData) {
                return (HttpData) data;
            }
        } catch (Throwable e) {
            Exceptions.handle(WebServer.LOG, e);
        }
        return null;
    }

    public FileUpload getFileUData(String key) {
        if (postDecoder == null) {
            return null;
        }
        try {
            InterfaceHttpData data = postDecoder.getBodyHttpData(key);
            if (data != null && data instanceof FileUpload) {
                return (FileUpload) data;
            }
        } catch (Throwable e) {
            Exceptions.handle(WebServer.LOG, e);
        }
        return null;
    }

    public void setAttribute(String key, Object value) {
        if (attribute == null) {
            attribute = Maps.newTreeMap();
        }
        attribute.put(key, value);
    }

    private void initSession() {
        session = Maps.newHashMap();
        String encodedSession = getCookieValue(sessionCookieName);
        if (Strings.isFilled(encodedSession)) {
            Tuple<String, String> sessionInfo = Strings.split(encodedSession, ":");
            if (Strings.areEqual(sessionInfo.getFirst(),
                                 Hashing.md5().hashString(sessionInfo.getSecond() + getSessionSecret()).toString())) {
                QueryStringDecoder qsd = new QueryStringDecoder(encodedSession);
                for (Map.Entry<String, List<String>> entry : qsd.getParameters().entrySet()) {
                    session.put(entry.getKey(), Iterables.getFirst(entry.getValue(), null));
                }
            } else {
                WebServer.LOG.FINE("Resetting client session due to security breach: %s", encodedSession);
            }
        }
    }

    public void setSessionValue(String key, Object value) {
        if (session == null) {
            initSession();
        }
        session.put(key, NLS.toMachineString(value));
        sessionModified = true;
    }

    public Value getSessionValue(String key) {
        if (session == null) {
            initSession();
        }
        return Value.of(session.get(key));
    }

    public ServerSession getServerSession(boolean create) {
        if (serverSession != null) {
            return serverSession;
        }
        String sid = null;
        if (serverSessionSource == null) {
            sid = getParameter(serverSessionParameterName);
            serverSessionSource = ServerSessionSource.PARAMETER;
            if (Strings.isEmpty(sid)) {
                serverSessionSource = ServerSessionSource.COOKIE;
                sid = getCookieValue(serverSessionCookieName);
                if (Strings.isEmpty(sid)) {
                    serverSessionSource = null;
                }
            }
        }
        if (Strings.isFilled(sid) || create) {
            serverSession = ServerSession.getSession(sid);
        }
        return serverSession;
    }

    public ServerSessionSource getServerSessionSource() {
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
        if (cookiesIn == null) {
            cookiesIn = Maps.newHashMap();
            CookieDecoder cd = new CookieDecoder();
            String cookies = request.getHeader(HttpHeaders.Names.COOKIE);
            if (Strings.isFilled(cookies)) {
                for (Cookie cookie : cd.decode(cookies)) {
                    this.cookiesIn.put(cookie.getName(), cookie);
                }
            }
        }

        return cookiesIn.get(name);
    }

    public String getCookieValue(String name) {
        Cookie c = getCookie(name);
        if (c == null) {
            return null;
        }
        return c.getValue();
    }

    public void setCookie(Cookie cookie) {
        if (cookiesOut == null) {
            cookiesOut = Maps.newTreeMap();
        }
        cookiesOut.put(cookie.getName(), cookie);
    }

    public void setSessionCookie(String name, String value) {
        setCookie(name, value, Integer.MIN_VALUE);
    }

    public void setCookie(String name, String value, int maxAgeSeconds) {
        DefaultCookie cookie = new DefaultCookie(name, value);
        cookie.setMaxAge((int) maxAgeSeconds);
        setCookie(cookie);
    }

    protected Collection<Cookie> getOutCookies() {
        if (serverSession != null && serverSession.isNew()) {
            setSessionCookie(serverSessionCookieName, serverSession.getId());
        }
        if (sessionModified) {
            QueryStringEncoder encoder = new QueryStringEncoder("");
            for (Map.Entry<String, String> e : session.entrySet()) {
                encoder.addParam(e.getKey(), e.getValue());
            }
            String value = encoder.toString();
            String protection = Hashing.md5().hashString(value + getSessionSecret()).toString();
            setSessionCookie(sessionCookieName, protection + ":" + value);
        }
        return cookiesOut == null ? null : cookiesOut.values();
    }

    private String getSessionSecret() {
        if (Strings.isFilled(sessionSecret)) {
            sessionSecret = UUID.randomUUID().toString();
        }
        return sessionSecret;
    }

    public Response respondWith() {
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

    public String getContextPrefix() {
        return contextPrefix;
    }

    public HttpPostRequestDecoder getPostDecoder() {
        return postDecoder;
    }

    void setPostDecoder(HttpPostRequestDecoder postDecoder) {
        this.postDecoder = postDecoder;
    }

    void setContent(Attribute content) {
        this.content = content;
    }

    public Attribute getContent() {
        return content;
    }

    public StructuredInput getXMLContent() {
        try {
            if (content == null) {
                throw Exceptions.handle()
                                .to(WebServer.LOG)
                                .withSystemErrorMessage("Expected valid XML as body of this request.")
                                .handle();
            }
            if (content.isInMemory()) {
                return new XMLStructuredInput(new ByteArrayInputStream(content.get()), true);
            } else {
                if (content.getFile().length() > maxStructuredInputSize && maxStructuredInputSize > 0) {
                    throw Exceptions.handle()
                                    .to(WebServer.LOG)
                                    .withSystemErrorMessage(
                                            "Request body is too large to parse as XML. The limit is %d bytes",
                                            maxStructuredInputSize)
                                    .handle();
                }
                return new XMLStructuredInput(new FileInputStream(content.getFile()), true);
            }
        } catch (HandledException e) {
            throw e;
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(WebServer.LOG)
                            .error(e)
                            .withSystemErrorMessage("Expected valid XML as body of this request: %s (%s).")
                            .handle();
        }
    }

    public Map<String, Object> getJSONContent() {
        try {
            if (content == null) {
                throw Exceptions.handle()
                                .to(WebServer.LOG)
                                .withSystemErrorMessage("Expected a valid JSON map as body of this request.")
                                .handle();
            }
            if (content.isInMemory()) {
                return mapper.readValue(new ByteArrayInputStream(content.get()), Map.class);
            } else {
                if (content.getFile().length() > maxStructuredInputSize && maxStructuredInputSize > 0) {
                    throw Exceptions.handle()
                                    .to(WebServer.LOG)
                                    .withSystemErrorMessage(
                                            "Request body is too large to parse as JSON. The limit is %d bytes",
                                            maxStructuredInputSize)
                                    .handle();
                }
                return mapper.readValue(content.getFile(), Map.class);
            }
        } catch (HandledException e) {
            throw e;
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(WebServer.LOG)
                            .error(e)
                            .withSystemErrorMessage("Expected a valid JSON map as body of this request: %s (%s).")
                            .handle();
        }
    }

    public void release() {
        if (postDecoder != null) {
            try {
                postDecoder.cleanFiles();
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
            postDecoder = null;
        }
        if (content != null) {
            try {
                content.delete();
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
            content = null;
        }
    }
}
