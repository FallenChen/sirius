package sirius.web.http.services;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.Async;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Callback;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.annotations.Context;
import sirius.kernel.di.annotations.Register;
import sirius.kernel.health.Exceptions;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 11:49
 * To change this template use File | Settings | File Templates.
 */
@Register
public class ServiceDispatcher implements WebDispatcher {
    @Override
    public int getPriority() {
        return PriorityCollector.DEFAULT_PRIORITY;
    }

    @Context
    private GlobalContext gc;

    @Override
    public boolean dispatch(final WebContext ctx) throws Exception {
        // We use the translated URI because legacy /services might have been routed elsewhere.
        if (!ctx.getRequestedURI().startsWith("/service")) {
            return false;
        }
        String uri = ctx.getRequestedURI();
        if ("/service".equals(uri)) {
            ctx.respondWith().cached().template("/help/service/info.xhtml");
        }
        final String[] path = uri.substring(1).split("/");
        if (path.length != 3) {
            ctx.respondWith()
               .cached()
               .template("/help/service/info.xhtml",
                         Strings.apply("Invalid call format: %s. Use /service/<type>/<service>.", uri));
            return true;
        } else {
            ctx.onComplete(new Callback<CallContext>() {
                @Override
                public void invoke(CallContext value) throws Exception {
                    value.getWatch().submitMicroTiming(ctx.getRequestedURI());
                }
            });
            Async.executor("web-services").fork(new Runnable() {
                @Override
                public void run() {
                    ServiceCall call = null;
                    String type = path[1];
                    String[] subURI = new String[path.length - 2];
                    System.arraycopy(path, 2, subURI, 0, path.length - 2);
                    if ("xml".equals(type)) {
                        call = new XMLServiceCall(subURI, ctx);
                    } else if ("json".equals(type)) {
                        call = new JSONServiceCall(subURI, ctx);
                    }

                    if (call == null) {
                        ctx.respondWith()
                           .error(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE,
                                  Exceptions.createHandled()
                                            .withSystemErrorMessage(
                                                    "Unknown or unsupported type: %s. Use 'xml' or 'json'",
                                                    type)
                                            .handle());
                        return;
                    }
                    String service = path[2];
                    StructuredService serv = gc.getPart(service, StructuredService.class);
                    if (serv == null) {
                        call.handle(null,
                                    Exceptions.createHandled()
                                              .withSystemErrorMessage(
                                                      "Unknown service: %s. Try /services for a complete list of available services.",
                                                      service)
                                              .handle());
                    } else {
                        call.invoke(serv);
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
    }
}
