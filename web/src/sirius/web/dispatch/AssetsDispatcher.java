/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.dispatch;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.commons.PriorityCollector;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.Tuple;
import sirius.kernel.di.std.Register;
import sirius.web.http.WebContext;
import sirius.web.http.WebDispatcher;

import java.io.File;
import java.net.URL;

/**
 * Dispatches all URLs below <code>/assets</code>.
 * <p>
 * All assets are fetched from the classpath and should be located in the <tt>resources</tt> source root (below the
 * <tt>assets</tt> directory).
 * </p>
 * <p>
 * This dispatcher tries to support caching as well as zero-copy delivery of static files if possible.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/11
 */
@Register
public class AssetsDispatcher implements WebDispatcher {

    @Override
    public int getPriority() {
        return PriorityCollector.DEFAULT_PRIORITY;
    }

    @Override
    public boolean preDispatch(WebContext ctx) throws Exception {
        return false;
    }

    @Override
    public boolean dispatch(WebContext ctx) throws Exception {
        if (!ctx.getRequest().getUri().startsWith("/assets") || HttpMethod.GET != ctx.getRequest().getMethod()) {
            return false;
        }
        String uri = ctx.getRequestedURI();
        if (uri.startsWith("/assets/dynamic")) {
            uri = uri.substring(16);
            Tuple<String, String> pair = Strings.split(uri, "/");
            uri = "/assets/" + pair.getSecond();
        }
        URL url = getClass().getResource(uri);
        if (url == null) {
            url = getClass().getResource("/assets/defaults" + uri.substring(7));
        }
        if (url == null) {
            ctx.respondWith().error(HttpResponseStatus.NOT_FOUND);
        } else if ("file".equals(url.getProtocol())) {
            ctx.respondWith().file(new File(url.toURI()));
        } else {
            ctx.respondWith().resource(url.openConnection());
        }
        return true;
    }
}
