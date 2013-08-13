package sirius.web.controller;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.Async;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Callback;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.di.Injector;
import sirius.kernel.di.std.Register;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.health.Log;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;
import sirius.web.http.WebServer;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 14:25
 * To change this template use File | Settings | File Templates.
 */
@Register
public class ControllerDispatcher implements WebDispatcher {

    protected static final Log LOG = Log.get("controller");

    private List<Route> routes;

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

    public boolean route(final WebContext ctx) {
        for (final Route route : routes) {
            try {
                final List<Object> params = route.matches(ctx, ctx.getRequestedURI());
                if (params != null) {
                    Async.executor("web-mvc").fork(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                params.add(0, ctx);
                                route.getSuccessCallback().invoke(params);
                            } catch (Throwable ex) {
                                ctx.respondWith()
                                   .error(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                          Exceptions.handle(WebServer.LOG, ex));
                            }
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
                        try {
                            route.getFailureCallback().invoke(Exceptions.handle(WebServer.LOG, e));
                        } catch (Throwable ex) {
                            ctx.respondWith()
                               .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, ex));
                        }
                    }


                }).execute();
                return true;
            }
        }
        return false;
    }

    private void buildRouter() {
        PriorityCollector<Route> collector = PriorityCollector.create();
        for (final Controller controller : Injector.context().getParts(Controller.class)) {
            Method defaultHandler = null;
            for (final Method m : controller.getClass().getMethods()) {
                if (m.isAnnotationPresent(Routed.class) && m.getAnnotation(Routed.class).defaultHandler()) {
                    if (m.getParameterTypes().length == 1 && WebContext.class.equals(m.getParameterTypes()[0])) {
                        defaultHandler = m;
                    } else {
                        LOG.WARN(
                                "Cannot used '%s' as default route in controller '%s' - Method need to be accessible and must have exactly one parameter of type: %s",
                                m.getName(),
                                controller.getClass().getName(),
                                WebContext.class.getName());
                    }
                }
            }
            for (final Method m : controller.getClass().getMethods()) {
                if (!m.isAnnotationPresent(Routed.class)) {
                    break;
                }
                Routed routed = m.getAnnotation(Routed.class);
                Route route = compileMethod(routed.value(), controller, m, defaultHandler);
                if (route != null) {
                    collector.add(routed.priority(), route);
                }
            }
        }

        routes = collector.getData();
    }

    private Route compileMethod(String uri, final Controller controller, final Method m, final Method defaultHandler) {
        try {
            final Route route = Route.compile(uri, m.getParameterTypes());
            route.setFailureCallback(new Callback<HandledException>() {
                @Override
                public void invoke(HandledException value) throws Exception {
                    CallContext.getCurrent()
                               .addToMDC("controller", controller.getClass().getName() + "." + m.getName());
                    WebContext ctx = CallContext.getCurrent().get(WebContext.class);
                    if (defaultHandler != null) {
                        //ctx. TODO add error
                        try {
                            defaultHandler.invoke(controller, ctx);
                        } catch (Throwable e) {
                            CallContext.getCurrent()
                                       .addToMDC("handler",
                                                 controller.getClass().getName() + "." + defaultHandler.getName());
                            controller.onError(ctx, Exceptions.handle(ControllerDispatcher.LOG, e));
                        }
                    } else {
                        controller.onError(ctx, value);
                    }
                }
            });
            route.setSuccessCallback(new Callback<List<Object>>() {
                @Override
                public void invoke(List<Object> params) throws Exception {
                    try {
                        m.invoke(controller, params.toArray());
                    } catch (Throwable e) {
                        route.getFailureCallback().invoke(Exceptions.handle(ControllerDispatcher.LOG, e));
                    }
                }
            });
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
