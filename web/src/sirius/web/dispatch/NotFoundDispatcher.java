package sirius.web.dispatch;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.commons.Strings;
import sirius.kernel.di.std.Register;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

/**
 * Sends a 404 (not found) for all unhandled URIs.
 * <p>
 * If no other dispatcher jumps in, this will take care of handing the request by sending a HTTP/404.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register
public class NotFoundDispatcher implements WebDispatcher {
    @Override
    public int getPriority() {
        return 999;
    }

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        ctx.respondWith()
           .error(HttpResponseStatus.NOT_FOUND,
                  Strings.apply("No dispatcher found for: %s", ctx.getRequest().getUri()));
        return true;
    }
}
