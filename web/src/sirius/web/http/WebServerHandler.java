package sirius.web.http;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import sirius.kernel.async.CallContext;
import sirius.kernel.health.Exceptions;

import java.util.List;

public class WebServerHandler extends SimpleChannelUpstreamHandler {

    private List<WebDispatcher> sortedDispatchers;

    public WebServerHandler(List<WebDispatcher> sortedDispatchers) {
        this.sortedDispatchers = sortedDispatchers;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (ctx.getAttachment() != null) {
            CallContext.setCurrent((CallContext) ctx.getAttachment());
        }
        Exceptions.handle(WebServer.LOG, e.getCause());
        e.getChannel().close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        try {
            HttpRequest req = (HttpRequest) e.getMessage();
            WebContext wc = setupContext(ctx, req);
            try {
                for (WebDispatcher wd : sortedDispatchers) {
                    if (wd.dispatch(wc)) {
                        return;
                    }
                }
            } catch (Exception ex) {
                Exceptions.handle(WebServer.LOG, ex);
                wc.respond().error(HttpResponseStatus.INTERNAL_SERVER_ERROR, ex);
            }
        } catch (Exception ex) {
            Exceptions.handle(WebServer.LOG, ex);
        }
        // This is practically dead code, because the NotFoundDispatcher will handle every request.
        e.getChannel().close();
    }

    private WebContext setupContext(ChannelHandlerContext ctx, HttpRequest req) {
        CallContext cc = CallContext.initialize();
        cc.addToMDC("uri", req.getUri());
        WebContext wc = cc.get(WebContext.class);
        wc.setCtx(ctx);
        wc.setRequest(req);
        ctx.setAttachment(cc);
        return wc;
    }

}
