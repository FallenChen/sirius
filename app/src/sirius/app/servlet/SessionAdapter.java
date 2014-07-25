/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.app.servlet;

import sirius.kernel.commons.Callback;
import sirius.web.http.session.ServerSession;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13
 * Time: 21:51
 * To change this template use File | Settings | File Templates.
 */
public class SessionAdapter implements HttpSession {

    private ServerSession session;
    private ServletContainer ctx;

    public SessionAdapter(ServerSession session, ServletContainer ctx) {
        this.session = session;
        this.ctx = ctx;
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return ctx;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        // IGNORE
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String s) {
        return session.getValue(s).get();
    }

    @Override
    @Deprecated
    public Object getValue(String s) {
        return session.getValue(s).get();
    }

    @Override
    public Enumeration getAttributeNames() {
        return Collections.enumeration(session.getKeys());
    }

    @Override
    @Deprecated
    public String[] getValueNames() {
        return session.getKeys().toArray(new String[session.getKeys().size()]);
    }

    @Override
    public void setAttribute(final String s, final Object o) {
        if (session.hasKey(s)) {
            ctx.invokeListeners("attributeReplaced",
                    HttpSessionAttributeListener.class,
                    new Callback<HttpSessionAttributeListener>() {
                        @Override
                        public void invoke(HttpSessionAttributeListener value) throws Exception {
                            value.attributeReplaced(new HttpSessionBindingEvent(SessionAdapter.this, s, o));
                        }
                    }
            );
        } else {
            ctx.invokeListeners("attributeAdded",
                    HttpSessionAttributeListener.class,
                    new Callback<HttpSessionAttributeListener>() {
                        @Override
                        public void invoke(HttpSessionAttributeListener value) throws Exception {
                            value.attributeAdded(new HttpSessionBindingEvent(SessionAdapter.this, s, o));
                        }
                    }
            );
        }
        session.putValue(s, o);
    }

    @Override
    @Deprecated
    public void putValue(String s, Object o) {
        session.putValue(s, o);
    }

    @Override
    public void removeAttribute(final String s) {
        ctx.invokeListeners("attributeRemoved",
                HttpSessionAttributeListener.class,
                new Callback<HttpSessionAttributeListener>() {
                    @Override
                    public void invoke(HttpSessionAttributeListener value) throws Exception {
                        value.attributeRemoved(new HttpSessionBindingEvent(SessionAdapter.this, s));
                    }
                }
        );
        session.removeValue(s);
    }

    @Override
    @Deprecated
    public void removeValue(String s) {
        session.removeValue(s);
    }

    @Override
    public void invalidate() {
        session.invalidate();
    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }
}
