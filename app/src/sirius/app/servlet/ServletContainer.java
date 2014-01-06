package sirius.app.servlet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.Sirius;
import sirius.kernel.async.Async;
import sirius.kernel.commons.Callback;
import sirius.kernel.di.Lifecycle;
import sirius.kernel.di.std.ConfigValue;
import sirius.kernel.di.std.Register;
import sirius.kernel.extensions.Extension;
import sirius.kernel.extensions.Extensions;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.http.MimeHelper;
import sirius.web.http.ServerSession;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 08:46
 * To change this template use File | Settings | File Templates.
 */
@Register(classes = {Lifecycle.class, WebDispatcher.class, ServerSession.Listener.class})
public class ServletContainer implements Lifecycle, ServletContext, WebDispatcher, ServerSession.Listener {

    private Map<String, Servlet> servlets = Maps.newHashMap();
    private List<Object> listeners = Lists.newArrayList();
    private Map<String, String> contextParams = Maps.newTreeMap();
    private Map<String, Object> attributes = Maps.newTreeMap();
    protected static final Log LOG = Log.get("servlets");

    @ConfigValue("servlet.majorVersion")
    private int majorVersion;

    @ConfigValue("servlet.minorVersion")
    private int minorVersion;

    protected <L> void invokeListeners(String message, Class<L> type, Callback<L> callback) {
        for (Object l : listeners) {
            if (type.isAssignableFrom(l.getClass())) {
                try {
                    callback.invoke((L) l);
                } catch (Exception e) {
                    Exceptions.handle()
                              .error(e)
                              .to(LOG)
                              .withSystemErrorMessage("Cannot send %s to listener: %s (%s) - %s (%s)",
                                                      message,
                                                      l,
                                                      l.getClass())
                              .handle();
                }
            }
        }
    }

    @Override
    public void started() {
        // Load context-params...
        for (Map.Entry<String, com.typesafe.config.ConfigValue> entry : Sirius.getConfig()
                                                                              .getConfig("servlet.params")
                                                                              .entrySet()) {
            try {
                contextParams.put(entry.getKey(), (String) entry.getValue().unwrapped());
            } catch (Exception e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Cannot read context-param: %s - %s (%s)", entry.getKey())
                          .handle();
            }
        }

        // Load listeners...
        for (Extension listenerDescriptor : Extensions.getExtensions("servlet.listeners")) {
            try {
                LOG.INFO("Loading listener: %s", listenerDescriptor.getId());
                listeners.add(listenerDescriptor.make("class"));
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Cannot load listener: %s - %s (%s)", listenerDescriptor.getId())
                          .handle();
            }
        }

        // Notify listeners
        invokeListeners("contextInitialized", ServletContextListener.class, new Callback<ServletContextListener>() {
            @Override
            public void invoke(ServletContextListener value) throws Exception {
                value.contextInitialized(new ServletContextEvent(ServletContainer.this));
            }
        });

        // Load servlets...
        for (Extension servletDescriptor : Extensions.getExtensions("servlet.servlets")) {
            try {
                LOG.INFO("Loading servlet: %s", servletDescriptor.getId());
                servlets.put(servletDescriptor.require("path").asString(), (Servlet) servletDescriptor.make("class"));
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Cannot load servlet: %s - %s (%s)", servletDescriptor.getId())
                          .handle();
            }
        }

        // Initialize servlets...
        for (final Servlet s : servlets.values()) {
            try {
                s.init(new ServletConfig() {
                    @Override
                    public String getServletName() {
                        return s.getClass().getSimpleName();
                    }

                    @Override
                    public ServletContext getServletContext() {
                        return ServletContainer.this;
                    }

                    @Override
                    public String getInitParameter(String s) {
                        return ServletContainer.this.getInitParameter(s);
                    }

                    @Override
                    public Enumeration getInitParameterNames() {
                        return ServletContainer.this.getInitParameterNames();
                    }
                });
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Cannot initialize servlet: %s - %s (%s)", s)
                          .handle();
            }
        }
    }

    @Override
    public void stopped() {
        // Destroy servlets...
        for (final Servlet s : servlets.values()) {
            try {
                s.destroy();
            } catch (Throwable e) {
                Exceptions.handle()
                          .error(e)
                          .to(LOG)
                          .withSystemErrorMessage("Cannot destroy servlet: %s - %s (%s)", s)
                          .handle();
            }
        }

        // Notify listeners
        invokeListeners("contextDestroyed", ServletContextListener.class, new Callback<ServletContextListener>() {
            @Override
            public void invoke(ServletContextListener value) throws Exception {
                value.contextDestroyed(new ServletContextEvent(ServletContainer.this));
            }
        });
    }

    @Override
    public String getName() {
        return "servlets (Sirius ServletContainer)";
    }

    @Override
    public String getContextPath() {
        return getServletContextName();
    }

    @Override
    public ServletContext getContext(String s) {
        return this;
    }

    @Override
    public int getMajorVersion() {
        return majorVersion;
    }

    @Override
    public int getMinorVersion() {
        return minorVersion;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String s) {
        return MimeHelper.guessMimeType(s);
    }

    @Override
    public Set getResourcePaths(String s) {
        return null;
    }

    @Override
    public URL getResource(String s) throws MalformedURLException {
        return getClass().getResource(s);
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        return getClass().getResourceAsStream(s);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Servlet getServlet(String s) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Enumeration getServlets() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Enumeration getServletNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(String s) {
        LOG.INFO(s);
    }

    @Override
    @Deprecated
    public void log(Exception e, String s) {
        Exceptions.handle().error(e).to(LOG).withSystemErrorMessage("%s - %s (%s)", s).handle();
    }

    @Override
    public void log(String s, Throwable throwable) {
        Exceptions.handle().error(throwable).to(LOG).withSystemErrorMessage("%s - %s (%s)", s).handle();
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return "Sirius";
    }

    @Override
    public String getInitParameter(String s) {
        return contextParams.get(s);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(contextParams.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
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
    public void setAttribute(final String s, final Object o) {
        if (attributes.containsKey(s)) {
            invokeListeners("attributeReplaced",
                            ServletContextAttributeListener.class,
                            new Callback<ServletContextAttributeListener>() {
                                @Override
                                public void invoke(ServletContextAttributeListener value) throws Exception {
                                    value.attributeReplaced(new ServletContextAttributeEvent(ServletContainer.this,
                                                                                             s,
                                                                                             o));
                                }
                            });
        } else {
            invokeListeners("attributeAdded",
                            ServletContextAttributeListener.class,
                            new Callback<ServletContextAttributeListener>() {
                                @Override
                                public void invoke(ServletContextAttributeListener value) throws Exception {
                                    value.attributeAdded(new ServletContextAttributeEvent(ServletContainer.this, s, o));
                                }
                            });
        }
        attributes.put(s, o);
    }

    @Override
    public void removeAttribute(final String s) {
        invokeListeners("attributeRemoved",
                        ServletContextAttributeListener.class,
                        new Callback<ServletContextAttributeListener>() {
                            @Override
                            public void invoke(ServletContextAttributeListener value) throws Exception {
                                value.attributeRemoved(new ServletContextAttributeEvent(ServletContainer.this,
                                                                                        s,
                                                                                        null));
                            }
                        });
        attributes.remove(s);
    }

    @Override
    public String getServletContextName() {
        return "/";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean dispatch(final WebContext ctx) throws Exception {
        LOG.INFO(ctx.getRequestedURI());
        for (final Map.Entry<String, Servlet> s : servlets.entrySet()) {
            if (ctx.getRequestedURI().startsWith(s.getKey())) {
                final ResponseAdapter res = new ResponseAdapter(ctx);
                final RequestAdapter req = new RequestAdapter(ctx, s.getKey(), this, res);
                Async.executor("servlets").start(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            invokeListeners("requestInitialized",
                                    ServletRequestListener.class,
                                    new Callback<ServletRequestListener>() {
                                        @Override
                                        public void invoke(ServletRequestListener value) throws Exception {
                                            value.requestInitialized(new ServletRequestEvent(ServletContainer.this,
                                                    req));
                                        }
                                    });
                            try {
                                s.getValue().service(req, res);
                                if (!req.isAsyncStarted()) {
                                res.complete();
                                }
                            } finally {
                                invokeListeners("requestDestroyed",
                                        ServletRequestListener.class,
                                        new Callback<ServletRequestListener>() {
                                            @Override
                                            public void invoke(ServletRequestListener value) throws Exception {
                                                value.requestDestroyed(new ServletRequestEvent(ServletContainer.this,
                                                        req));
                                            }
                                        });
                            }
                        } catch (Throwable t) {
                            LOG.SEVERE(t);
                            if (!res.isCommitted()) {
                                ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(LOG, t));
                            }
                        }
                    }
                }).execute();
                return true;
            }
        }
        return false;
    }

    @Override
    public void sessionCreated(final ServerSession session) throws Exception {
        invokeListeners("sessionCreated", HttpSessionListener.class, new Callback<HttpSessionListener>() {
            @Override
            public void invoke(HttpSessionListener value) throws Exception {
                value.sessionCreated(new HttpSessionEvent(new SessionAdapter(session, ServletContainer.this)));
            }
        });
    }

    @Override
    public void sessionInvalidated(final ServerSession session) throws Exception {
        invokeListeners("sessionDestroyed", HttpSessionListener.class, new Callback<HttpSessionListener>() {
            @Override
            public void invoke(HttpSessionListener value) throws Exception {
                value.sessionDestroyed(new HttpSessionEvent(new SessionAdapter(session, ServletContainer.this)));
            }
        });
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getClassLoader() {
        return Sirius.getClasspath().getLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVirtualServerName() {
        return "Sirius";
    }
}
