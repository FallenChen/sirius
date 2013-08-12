package sirius.web.controller;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.health.HandledException;
import sirius.web.http.WebContext;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 27.07.13
 * Time: 16:25
 * To change this template use File | Settings | File Templates.
 */
public class BasicController implements Controller {
    @Override
    public void onError(WebContext ctx, HandledException error) {
        ctx.respondWith().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
    }
}
