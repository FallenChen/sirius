/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.multipart.Attribute;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;

import java.io.File;
import java.nio.channels.ClosedChannelException;
import java.util.List;

/**
 * Handles incoming HTTP requests.
 * <p>
 * Takes care of gluing together chunks, handling file uploads etc. In order to participate in handling HTTP requests,
 * one has to provide a {@link WebDispatcher} rather than modifying this class.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
class WebServerHandler extends IdleStateAwareChannelUpstreamHandler {

    private List<WebDispatcher> sortedDispatchers;
    private int numKeepAlive = 5;
    private boolean readingChunks;
    //    private String proxyAddress;
    private WebContext currentContext;


    /**
     * Creates a new instance based on a pre sorted list of dispatchers.
     *
     * @param sortedDispatchers the sorted list of dispatchers responsible for handling HTTP requests.
     */
    WebServerHandler(List<WebDispatcher> sortedDispatchers) {
        this.sortedDispatchers = sortedDispatchers;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        WebServer.openConnections.incrementAndGet();
//        if (firewall.isActive()) {
//            InetSocketAddress address = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
//            if (!firewall.accepts(address)) {
//                ctx.getChannel().close();
//            } else {
//                super.channelOpen(ctx, e);
//            }
//        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        cleanup();
        WebServer.openConnections.decrementAndGet();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (ctx.getAttachment() != null) {
            CallContext.setCurrent((CallContext) ctx.getAttachment());
        }
        if (e instanceof ClosedChannelException) {
            WebServer.LOG.FINE(e);
        } else {
            Exceptions.handle(WebServer.LOG, e.getCause());
            e.getChannel().close();
        }
    }

    /*
     * Binds the request to the CallContext
     */
    private WebContext setupContext(ChannelHandlerContext ctx, HttpRequest req) {
        CallContext cc = CallContext.initialize();
        cc.addToMDC("uri", req.getUri());
        WebContext wc = cc.get(WebContext.class);
        wc.setCtx(ctx);
        wc.setRequest(req);
        ctx.setAttachment(cc);
        return wc;
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        // TODO handle
    }

    /**
     * Used by responses to determine if keepalive is supported.
     * <p>
     * Internally we used a countdown, to limit the max number of keepalives for a connection. Calling this method
     * decrements the internal counter, therefore this must not be called several times per request.
     * </p>
     *
     * @return <tt>true</tt> if keepalive is still supported, <tt>false</tt> otherwise.
     */
    public boolean shouldKeepAlive() {
        return numKeepAlive-- > 0;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (readingChunks) {
            continueChunkedRequest(ctx, e);
            WebServer.chunks++;
            if (WebServer.chunks < 0) {
                WebServer.chunks = 0;
            }
            return;
        } else {
            handleNewRequest(ctx, e);
            WebServer.requests++;
            if (WebServer.requests < 0) {
                WebServer.requests = 0;
            }
        }
    }

    /*
     * Handles a new request - called once the first chunk of data of a request is available.
     */
    private void handleNewRequest(ChannelHandlerContext ctx, MessageEvent e) {
        try {
            cleanup();
            HttpRequest req = (HttpRequest) e.getMessage();
            currentContext = setupContext(ctx, req);
            try {
//                if (firewall.isActive() && proxyAddress != null) {
//                    if (firewall.accepts(currentContext.getRemoteAddress())) {
//                        ctx.getChannel().close();
//                        return;
//                    }
//                }
                if (req.getMethod() == HttpMethod.GET || req.getMethod() == HttpMethod.HEAD || req.getMethod() == HttpMethod.DELETE) {
                    if (HttpHeaders.is100ContinueExpected(req)) {
                        send100Continue(e);
                    }
                    handleGETorHEADorDELETE(req);
                } else if (req.getMethod() == HttpMethod.POST || req.getMethod() == HttpMethod.PUT) {
                    if (HttpHeaders.is100ContinueExpected(req)) {
                        send100Continue(e);
                    }
                    handlePOSTandPUT(req);
                } else {
                    currentContext.respondWith()
                                  .error(HttpResponseStatus.BAD_REQUEST,
                                         Strings.apply("Cannot %s as method. Use GET, POST, PUT, HEAD, DELETE",
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

    private void send100Continue(MessageEvent e) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        e.getChannel().write(response);
    }

    /*
     * Handles POST or PUT request - those are the only methods where we expect uploaded content.
     */
    private boolean handlePOSTandPUT(HttpRequest req) throws Exception {
        try {
            String contentType = req.getHeader(HttpHeaders.Names.CONTENT_TYPE);
            if (Strings.isFilled(contentType) && (contentType.startsWith("multipart/form-data") || contentType.startsWith(
                    "application/x-www-form-urlencoded"))) {
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(WebServer.getHttpDataFactory(), req);
                currentContext.setPostDecoder(postDecoder);
            } else {
                Attribute body = WebServer.getHttpDataFactory().createAttribute(req, "body");
                body.setContent(req.getContent());
                currentContext.content = body;
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

    /*
     * Handles GET, HEAD or DELETE requests, where we don't expect any content
     */
    private boolean handleGETorHEADorDELETE(HttpRequest req) throws Exception {
        if (req.isChunked()) {
            currentContext.respondWith()
                          .error(HttpResponseStatus.BAD_REQUEST,
                                 "Cannot handle chunked GET, HEAD or DELETE. Use POST or PUT");
            return true;
        }
        dispatch();
        return false;
    }

    /*
     * Releases the last context (request) which was processed by this handler.
     */
    private void cleanup() {
        if (currentContext != null) {
            currentContext.release();
            currentContext = null;
        }
    }

    /*
     * Reads another chunk of data for a previously started request
     */
    private void continueChunkedRequest(ChannelHandlerContext ctx, MessageEvent e) {
        HttpChunk chunk = (HttpChunk) e.getMessage();
        try {
            if (currentContext.getPostDecoder() != null) {
                currentContext.getPostDecoder().offer(chunk);
            } else {
                currentContext.content.addContent(chunk.getContent(), chunk.isLast());
                if (!currentContext.content.isInMemory()) {
                    File file = currentContext.content.getFile();
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

    /*
     * Checks if the can still upload more date
     */
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

    /*
     * Dispatches the completely read request.
     */
    private void dispatch() throws Exception {
        for (WebDispatcher wd : sortedDispatchers) {
            try {
                if (wd.dispatch(currentContext)) {
                    return;
                }
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
        }
    }


}
