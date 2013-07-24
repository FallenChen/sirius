package sirius.web.http.dispatch;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.di.annotations.Register;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

import java.io.File;
import java.net.URL;

@Register
public class AssetsDispatcher implements WebDispatcher {
    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        if (!ctx.getRequest().getUri().startsWith("/assets") || HttpMethod.GET != ctx.getRequest().getMethod()) {
            return false;
        }
        if (ctx.getRequestedURI().startsWith("/assets/dynamic")) {
            ctx.respond().cache().template(ctx.getRequestedURI());
        } else {
            URL url = getClass().getResource(ctx.getRequestedURI());
            if (url == null) {
                url = getClass().getResource("/assets/defaults" + ctx.getRequestedURI().substring(7));
            }
            if (url == null) {
                ctx.respond().error(HttpResponseStatus.NOT_FOUND);
            } else if ("file".equals(url.getProtocol())) {
                ctx.respond().file(new File(url.toURI()));
            } else {
                ctx.respond().resource(url.openConnection());
            }
        }
        return true;
    }
}
