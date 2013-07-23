package sirius.app.servlet;

import com.google.common.collect.Maps;
import org.jboss.netty.util.CharsetUtil;
import sirius.web.WebContext;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

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

    public RequestAdapter(WebContext ctx, String servletPath, ServletContainer container) {
        this.ctx = ctx;
        this.servletPath = servletPath;
        this.container = container;
    }

    @Override
    public String getAuthType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getQueryString() {
        return ctx.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserInRole(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestedSessionId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return ctx.getServerSessionSource() == WebContext.SERVER_SESSION_SOURCE_COOKIE;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return ctx.getServerSessionSource() == WebContext.SERVER_SESSION_SOURCE_PARAMETER;
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
        return CharsetUtil.UTF_8.name();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return 0;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScheme() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServerName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getServerPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(""));
    }

    @Override
    public String getRemoteAddr() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRemoteHost() {
        throw new UnsupportedOperationException();
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
        return Locale.US;
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
