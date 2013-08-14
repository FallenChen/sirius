package sirius.web.http;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;

import java.io.File;
import java.util.List;

public class WebServerHandler extends SimpleChannelUpstreamHandler {

    private List<WebDispatcher> sortedDispatchers;
    private int numKeepAlive = 3;
    private boolean readingChunks;

    private WebContext currentContext;

    public WebServerHandler(List<WebDispatcher> sortedDispatchers) {
        this.sortedDispatchers = sortedDispatchers;
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        cleanup();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (ctx.getAttachment() != null) {
            CallContext.setCurrent((CallContext) ctx.getAttachment());
        }
        Exceptions.handle(WebServer.LOG, e.getCause());
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

    public boolean shouldKeepAlive() {
        return numKeepAlive-- > 0;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (readingChunks) {
            continueChunkedRequest(ctx, e);
            return;
        } else {
            handleNewRequest(ctx, e);
        }
    }

    private void handleNewRequest(ChannelHandlerContext ctx, MessageEvent e) {
        try {
            cleanup();
            HttpRequest req = (HttpRequest) e.getMessage();
            currentContext = setupContext(ctx, req);
            try {
                if (req.getMethod() == HttpMethod.GET) {
                    handleGET(req);
                } else if (req.getMethod() == HttpMethod.POST || req.getMethod() == HttpMethod.PUT) {
                    handlePOSTandPUT(req);
                } else {
                    currentContext.respondWith()
                                  .error(HttpResponseStatus.BAD_REQUEST,
                                         Strings.apply("Cannot %s as method. Use GET, POST or PUT",
                                                       req.getMethod().getName()));
                }
            } catch (Throwable t) {
                currentContext.respondWith()
                              .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, t));
            }
        } catch (Throwable t) {
            Exceptions.handle(WebServer.LOG, t);
            try {
                ctx.getChannel().close();
            } catch (Exception ex) {
                Exceptions.ignore(ex);
            }
        }
    }

    private boolean handlePOSTandPUT(HttpRequest req) throws Exception {
        try {
            HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(WebServer.getHttpDataFactory(), req);
            if (!postDecoder.getBodyHttpDatas().isEmpty()) {
                currentContext.setPostDecoder(postDecoder);
            } else {
                currentContext.setContent(WebServer.getHttpDataFactory().createAttribute(req, "body"));
                currentContext.getContent().setContent(req.getContent());
            }

        } catch (Throwable ex) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, ex));
            return true;
        }
        if (req.isChunked()) {
            readingChunks = true;
        } else {
            dispatch();
        }
        return false;
    }

    private boolean handleGET(HttpRequest req) throws Exception {
        if (req.isChunked()) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.BAD_REQUEST, "Cannot handle chunked GET. Use POST or PUT");
            return true;
        }
        dispatch();
        return false;
    }

    private void cleanup() {
        if (currentContext != null) {
            currentContext.release();
            currentContext = null;
        }
    }

    private void continueChunkedRequest(ChannelHandlerContext ctx, MessageEvent e) {
        HttpChunk chunk = (HttpChunk) e.getMessage();
        try {
            if (currentContext.getPostDecoder() != null) {
                currentContext.getPostDecoder().offer(chunk);
                //TODO
//                if (currentContext.getPostDecoder().hasNext()) {
//                    currentContext.getPostDecoder().next().
//                }
            } else {
                currentContext.getContent().addContent(chunk.getContent(), chunk.isLast());
                if (!currentContext.getContent().isInMemory()) {
                    File file = currentContext.getContent().getFile();
                     checkUploadFileLimits(file);
                }
            }
            if (chunk.isLast()) {
                readingChunks = false;
                dispatch();
            }
        } catch (Throwable ex) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, ex));
        }
    }

    private void checkUploadFileLimits(File file) {
        if (file.getFreeSpace() < WebServer.getMinUploadFreespace() && WebServer.getMinUploadFreespace() > 0) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.INSUFFICIENT_STORAGE,
                                 Exceptions.handle()
                                           .withSystemErrorMessage(
                                                   "The web server is running out of temporary space to store the upload")
                                           .to(WebServer.LOG)
                                           .handle());
        }
        if (file.length() > WebServer.getMaxUploadSize() && WebServer.getMaxUploadSize() > 0) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.INSUFFICIENT_STORAGE,
                                 Exceptions.handle()
                                           .withSystemErrorMessage(
                                                   "The uploaded file exceeds the maximal upload size of %d bytes",
                                                   WebServer.getMaxUploadSize())
                                           .to(WebServer.LOG)
                                           .handle());
        }
    }

    private void dispatch() throws Exception {
        for (WebDispatcher wd : sortedDispatchers) {
            if (wd.dispatch(currentContext)) {
                return;
            }
        }
    }
}
