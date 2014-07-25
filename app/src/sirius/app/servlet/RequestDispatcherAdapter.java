/*
 * Copyright (c) 2014, all rights reserved.
 *
 * Made with all the love in the world by scireum GmbH in Remshalden, Germany
 *
 * www.scireum.de
 */
package sirius.app.servlet;

import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.commons.Tuple;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by aha on 11.07.14.
 */
public class RequestDispatcherAdapter implements RequestDispatcher, FilterChain {

    private ServletContainer container;
    private final String uri;
    private boolean includeFilters;
    private int index = 0;

    public RequestDispatcherAdapter(ServletContainer container, String uri, boolean includeFilters) {
        this.container = container;
        this.uri = uri;
        this.includeFilters = includeFilters;
    }

    @Override
    public void forward(ServletRequest servletRequest,
                        ServletResponse servletResponse) throws ServletException, IOException {
        ((RequestAdapter) servletRequest).setUri(uri);
        doFilter(servletRequest, servletResponse);
    }

    @Override
    public void include(ServletRequest servletRequest,
                        ServletResponse servletResponse) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse) throws IOException, ServletException {
        Filter f = includeFilters ? nextFilter(servletRequest) : null;
        if (f == null) {
            for (Tuple<String, Servlet> servlet : container.servlets) {
                if (match(servlet.getFirst(), ((RequestAdapter) servletRequest).getRequestURI())) {
                    ((RequestAdapter) servletRequest).setServletPath(servlet.getFirst());
                    servlet.getSecond().service(servletRequest, servletResponse);
                    return;
                }
            }
            ((RequestAdapter) servletRequest).ctx.respondWith().error(HttpResponseStatus.NOT_FOUND);
        } else {
            f.doFilter(servletRequest, servletResponse, this);
        }
    }

    private boolean match(String pattern, String requestURI) {
        if (pattern.endsWith("*")) {
            return requestURI.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return requestURI.endsWith(pattern.substring(1));
        }
        return requestURI.startsWith(pattern);
    }

    private Filter nextFilter(ServletRequest servletRequest) {
        while (index < container.filters.size()) {
            Tuple<String, Filter> f = container.filters.get(index++);
            if (match(f.getFirst(), ((RequestAdapter) servletRequest).getRequestURI())) {
                return f.getSecond();
            }
        }
        return null;
    }
}
