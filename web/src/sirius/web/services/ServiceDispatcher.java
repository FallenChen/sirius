package sirius.web.services;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.Async;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.GlobalContext;
import sirius.kernel.di.std.Context;
import sirius.kernel.di.std.Register;
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
            ctx.respondWith().cached().template("/view/help/service/info.xhtml");
            return true;
        }
        // Cut /service/
        final String subpath = uri.substring(9);
        Async.executor("web-services").fork(new Runnable() {
            @Override
            public void run() {
                ServiceCall call = null;
                Tuple<String,String> callPath = Strings.split(subpath,"/");
                String type = callPath.getFirst();
                String service = callPath.getSecond();
                if ("xml".equals(type)) {
                    call = new XMLServiceCall(ctx);
                } else if ("json".equals(type)) {
                    call = new JSONServiceCall(ctx);
                }

                if (call == null) {
                    ctx.respondWith()
                       .error(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE,
                              Exceptions.createHandled()
                                        .withSystemErrorMessage("Unknown or unsupported type: %s. Use 'xml' or 'json'",
                                                                type)
                                        .handle());
                    return;
                }
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
                ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Request dropped - System overload!");
            }
        }).execute();
        return true;
    }

}
