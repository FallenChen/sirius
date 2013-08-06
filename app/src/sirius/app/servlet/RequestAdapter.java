package sirius.app.servlet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import sirius.kernel.commons.Strings;
import sirius.web.http.MimeHelper;
import sirius.web.http.WebContext;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 17:38
 * To change this template use File | Settings | File Templates.
 */
public class RequestAdapter implements HttpServletRequest {

    private WebContext ctx;
    private String servletPath;
    private ServletContainer container;
    private Map<String, Object> attributes = Maps.newTreeMap();
    private SessionAdapter session;
    private List<Cookie> cookies;

    public RequestAdapter(WebContext ctx, String servletPath, ServletContainer container) {
        this.ctx = ctx;
        this.servletPath = servletPath;
        this.container = container;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            cookies = Lists.newArrayList();
            for (org.jboss.netty.handler.codec.http.Cookie cookie : ctx.getCookies()) {
                Cookie javaCookie = new Cookie(cookie.getName(), cookie.getValue());
                javaCookie.setComment(cookie.getComment());
                if (Strings.isFilled(cookie.getDomain())) {
                    javaCookie.setDomain(cookie.getDomain());
                }
                javaCookie.setMaxAge(cookie.getMaxAge());
                javaCookie.setPath(cookie.getPath());
                javaCookie.setSecure(cookie.isSecure());
                javaCookie.setVersion(cookie.getVersion());
                cookies.add(javaCookie);
            }
        }
        return cookies.toArray(new Cookie[cookies.size()]);
    }

    @Override
    public long getDateHeader(String s) {
        return ctx.getDateHeader(s);
    }

    @Override
    public String getHeader(String s) {
        return ctx.getRequest().getHeader(s);
    }

    @Override
    public Enumeration getHeaders(String s) {
        return Collections.enumeration(ctx.getRequest().getHeaders(s));
    }

    @Override
    public Enumeration getHeaderNames() {
        return Collections.enumeration(ctx.getRequest().getHeaderNames());
    }

    @Override
    public int getIntHeader(String s) {
        return Integer.parseInt(getHeader(s));
    }

    @Override
    public String getMethod() {
        return ctx.getRequest().getMethod().getName();
    }

    @Override
    public String getPathInfo() {
        String result = ctx.getRequestedURI().substring(getServletPath().length());
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        return result;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return ctx.getContextPrefix();
    }

    @Override
    public String getQueryString() {
        return ctx.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return ctx.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return ctx.getRequestedURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer();
        sb.append("http://localhost:9000"); //FIXME hardcoded
        sb.append(getRequestURI());
        return sb;
    }

    @Override
    public String getServletPath() {
        return "/".equals(servletPath) ? "" : servletPath;
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (session == null) {
            if (!create) {
                return null;
            }
            session = new SessionAdapter(ctx.getServerSession(), container);
        }
        return session;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return ctx.getServerSession(false) != null && !ctx.getServerSession()
                                                          .isNew() && ctx.getServerSessionSource() != null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return ctx.getServerSessionSource() == WebContext.ServerSessionSource.COOKIE;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return ctx.getServerSessionSource() == WebContext.ServerSessionSource.PARAMETER;
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public Object getAttribute(String s) {
        return attributes.get(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return ctx.getContent().getCharset().name();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        ctx.getContent().setCharset(Charset.forName(s));
    }

    @Override
    public int getContentLength() {
        return (int) ctx.getContent().length();
    }

    @Override
    public String getContentType() {
        return ctx.getHeaderValue(HttpHeaders.Names.CONTENT_TYPE)
                  .replaceEmptyWith(MimeHelper.guessMimeType(ctx.getRequestedURI()))
                  .asString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final InputStream is = (ctx.getContent().isInMemory()) ? new ByteArrayInputStream(ctx.getContent()
                                                                                             .get()) : new FileInputStream(
                ctx.getContent().getFile());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return is.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return is.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return is.read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return is.skip(n);
            }

            @Override
            public int available() throws IOException {
                return is.available();
            }

            @Override
            public void close() throws IOException {
                is.close();
            }

            @Override
            public synchronized void mark(int readlimit) {
                is.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                is.reset();
            }

            @Override
            public boolean markSupported() {
                return is.markSupported();
            }
        };
    }

    @Override
    public String getParameter(String s) {
        return ctx.getParameter(s);
    }

    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(ctx.getParameterNames());
    }

    @Override
    public String[] getParameterValues(String s) {
        return ctx.getParameters(s).toArray(new String[ctx.getParameters(s).size()]);
    }

    @Override
    public Map getParameterMap() {
        Map<String, String[]> result = Maps.newTreeMap();
        for (String s : ctx.getParameterNames()) {
            result.put(s, getParameterValues(s));
        }
        return result;
    }

    @Override
    public String getProtocol() {
        return ctx.getRequest().getProtocolVersion().getProtocolName();
    }

    @Override
    public String getScheme() {
        try {
            return new URI(ctx.getRequest().getUri()).getScheme();
        } catch (URISyntaxException e) {
            ServletContainer.LOG.FINE(e);
            return "unknown";
        }
    }

    @Override
    public String getServerName() {
        try {
            return new URI(ctx.getRequest().getUri()).getHost();
        } catch (URISyntaxException e) {
            ServletContainer.LOG.FINE(e);
            return "unknown";
        }
    }

    @Override
    public int getServerPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
    }

    @Override
    public String getRemoteAddr() {
        return ctx.getCtx().getChannel().getRemoteAddress().toString();
    }

    @Override
    public String getRemoteHost() {
        return getRemoteAddr();
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        attributes.remove(s);
    }

    @Override
    public Locale getLocale() {
        return new Locale(ctx.getLang());
    }

    @Override
    public Enumeration getLocales() {
        return Collections.enumeration(Collections.singleton(getLocale()));
    }

    @Override
    public boolean isSecure() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public String getRealPath(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemotePort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocalAddr() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }
}
