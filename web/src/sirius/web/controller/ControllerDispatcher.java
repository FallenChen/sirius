/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.controller;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.Async;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.di.Injector;
import sirius.kernel.di.std.Parts;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Dispatches incoming requests to the appropriate {@link Controller}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register
public class ControllerDispatcher implements WebDispatcher {

    protected static final Log LOG = Log.get("controller");

    private List<Route> routes;

    @Parts(Interceptor.class)
    private Collection<Interceptor> interceptors;

    @Override
    public int getPriority() {
        return PriorityCollector.DEFAULT_PRIORITY + 10;
    }

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        if (routes == null) {
            buildRouter();
        }

        return route(ctx);
    }

    private boolean route(final WebContext ctx) {
        for (final Route route : routes) {
            try {
                final List<Object> params = route.matches(ctx, ctx.getRequestedURI());
                if (params != null) {
                    Async.executor("web-mvc").fork(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                params.add(0, ctx);
                                for (Interceptor interceptor : interceptors) {
                                    if (interceptor.before(ctx, route.getController(), route.getSuccessCallback())) {
                                        return;
                                    }
                                }
                                route.getSuccessCallback().invoke(route.getController(), params.toArray());
                            } catch (InvocationTargetException ex) {
                                handleFailure(ctx, route, ex.getTargetException());
                            } catch (Throwable ex) {
                                handleFailure(ctx, route, ex);
                            }
                            ctx.enableTiming(route.toString());
                        }
                    }).dropOnOverload(new Runnable() {
                        @Override
                        public void run() {
                            ctx.respondWith()
                               .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Request dropped - System overload!");
                        }
                    }).execute();
                    return true;
                }
            } catch (final Throwable e) {
                Async.executor("web-mvc").fork(new Runnable() {
                    @Override
                    public void run() {
                        handleFailure(ctx, route, e);
                    }


                }).execute();
                return true;
            }
        }
        return false;
    }

    private void handleFailure(WebContext ctx, Route route, Throwable ex) {
        try {
            CallContext.getCurrent()
                       .addToMDC("controller",
                                 route.getController().getClass().getName() + "." + route.getSuccessCallback()
                                                                                         .getName());
            route.getController().onError(ctx, Exceptions.handle(ControllerDispatcher.LOG, ex));
        } catch (Throwable t) {
            ctx.respondWith()
               .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(ControllerDispatcher.LOG, t));
        }
    }

    private void buildRouter() {
        PriorityCollector<Route> collector = PriorityCollector.create();
        for (final Controller controller : Injector.context().getParts(Controller.class)) {
            for (final Method m : controller.getClass().getMethods()) {
                if (m.isAnnotationPresent(Routed.class)) {
                    Routed routed = m.getAnnotation(Routed.class);
                    Route route = compileMethod(routed.value(), controller, m);
                    if (route != null) {
                        collector.add(routed.priority(), route);
                    }
                }
            }
        }

        routes = collector.getData();
    }

    private Route compileMethod(String uri, final Controller controller, final Method m) {
        try {
            final Route route = Route.compile(uri, m.getParameterTypes());
            route.setController(controller);
            route.setSuccessCallback(m);
            return route;
        } catch (Throwable e) {
            LOG.WARN("Skipping '%s' in controller '%s' - Cannot compile route '%s': %s (%s)",
                     m.getName(),
                     controller.getClass().getName(),
                     uri,
                     e.getMessage(),
                     e.getClass().getName());
            return null;
        }
    }
}
