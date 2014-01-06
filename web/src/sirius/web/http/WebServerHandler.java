/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;

import java.io.File;
import java.io.IOException;
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
class WebServerHandler extends ChannelDuplexHandler {

    private List<WebDispatcher> sortedDispatchers;
    private int numKeepAlive = 5;
    private HttpRequest currentRequest;
    private WebContext currentContext;

    /**
     * Creates a new instance based on a pre sorted list of dispatchers.
     *
     * @param sortedDispatchers the sorted list of dispatchers responsible for handling HTTP requests.
     */
    WebServerHandler(List<WebDispatcher> sortedDispatchers) {
        this.sortedDispatchers = sortedDispatchers;
    }

    public static final AttributeKey<CallContext> CALL_CONTEXT_ATTRIBUTE_KEY = AttributeKey.valueOf("sirius.CallContext");

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        io.netty.util.Attribute<CallContext> attr = ctx.attr(CALL_CONTEXT_ATTRIBUTE_KEY);
        if (attr != null) {
            CallContext.setCurrent(attr.get());
        }
        if (e instanceof ClosedChannelException) {
            WebServer.LOG.FINE(e);
        } else if (e.getCause() instanceof IOException && "Connection reset by peer".equals(e
                .getMessage())) {
            WebServer.LOG.FINE(e);
        } else {
            Exceptions.handle(WebServer.LOG, e);
            try {
                if (ctx.channel().isOpen()) {
                    ctx.channel().close();
                }
            } catch (Throwable t) {
                Exceptions.ignore(t);
            }
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
        ctx.attr(CALL_CONTEXT_ATTRIBUTE_KEY).set(cc);
        return wc;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            io.netty.util.Attribute<CallContext> attr = ctx.attr(CALL_CONTEXT_ATTRIBUTE_KEY);
            if (attr != null) {
                CallContext callContext = attr.get();
                if (callContext == null) {
                    ctx.channel().close();
                    return;
                }
                CallContext.setCurrent(callContext);
                WebContext wc = callContext.get(WebContext.class);
                if (wc == null) {
                    ctx.channel().close();
                    return;
                }
                if (!wc.isLongCall()) {
                    if (WebServer.LOG.isFINE()) {
                        WebServer.LOG.FINE("IDLE: " + wc.getRequestedURI());
                    }
                    WebServer.idleTimeouts++;
                    if (WebServer.idleTimeouts < 0) {
                        WebServer.idleTimeouts = 0;
                    }
                    ctx.channel().close();
                    return;
                }
            }
        }
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
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelReadComplete(ctx);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        cleanup();
        WebServer.openConnections.decrementAndGet();
        super.disconnect(ctx, future);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            WebServer.requests++;
            if (WebServer.requests < 0) {
                WebServer.requests = 0;
            }

            currentRequest = (HttpRequest) msg;
            // Handle a bad request.
            if (!currentRequest.getDecoderResult().isSuccess()) {
                signalBadRequest(ctx);
                return;
            }
            boolean chunked = HttpHeaders.Values.CHUNKED.equals(currentRequest.headers().get(HttpHeaders.Names.TRANSFER_ENCODING));
            handleNewRequest(ctx, currentRequest, chunked);
        } else if (msg instanceof HttpContent) {
            if (currentRequest == null) {
                if (!(msg instanceof LastHttpContent)) {
                    signalBadRequest(ctx);
                }
                return;
            }
            continueChunkedRequest(ctx, msg);
            WebServer.chunks++;
            if (WebServer.chunks < 0) {
                WebServer.chunks = 0;
            }
        }
    }

    private void signalBadRequest(ChannelHandlerContext ctx) {
        WebServer.clientErrors++;
        if (WebServer.clientErrors < 0) {
            WebServer.clientErrors = 0;
        }
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)).addListener(ChannelFutureListener.CLOSE);
        currentRequest = null;
    }

    /*
     * Handles a new request - called once the first chunk of data of a request is available.
     */
    private void handleNewRequest(ChannelHandlerContext ctx, HttpRequest req, boolean chunked) {
        try {
            cleanup();
            if (WebServer.LOG.isFINE()) {
                WebServer.LOG.FINE("OPEN: " + req.getUri());
            }
            currentContext = setupContext(ctx, req);
            try {
                if (!WebServer.getIPFilter().isEmpty()) {
                    if (!WebServer.getIPFilter().accepts(currentContext.getRemoteIP())) {
                        WebServer.blocks++;
                        if (WebServer.blocks < 0) {
                            WebServer.blocks = 0;
                        }
                        if (WebServer.LOG.isFINE()) {
                            WebServer.LOG.FINE("BLOCK: " + req.getUri());
                        }
                        ctx.channel().close();
                        return;
                    }
                }

                if (req.getMethod() == HttpMethod.GET || req.getMethod() == HttpMethod.HEAD || req.getMethod() == HttpMethod.DELETE) {
                    if (HttpHeaders.is100ContinueExpected(req)) {
                        if (WebServer.LOG.isFINE()) {
                            WebServer.LOG.FINE("CONTINUE: " + req.getUri());
                        }
                        send100Continue(ctx);
                    }
                    handleGETorHEADorDELETE(req, chunked);
                } else if (req.getMethod() == HttpMethod.POST || req.getMethod() == HttpMethod.PUT) {
                    if (HttpHeaders.is100ContinueExpected(req)) {
                        if (WebServer.LOG.isFINE()) {
                            WebServer.LOG.FINE("CONTINUE: " + req.getUri());
                        }
                        send100Continue(ctx);
                    }
                    handlePOSTandPUT(req, chunked);
                } else {
                    currentContext.respondWith()
                            .error(HttpResponseStatus.BAD_REQUEST,
                                    Strings.apply("Cannot %s as method. Use GET, POST, PUT, HEAD, DELETE",
                                            req.getMethod().name()));
                }
            } catch (Throwable t) {
                currentContext.respondWith()
                        .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, t));
            }
        } catch (Throwable t) {
            Exceptions.handle(WebServer.LOG, t);
            try {
                ctx.channel().close();
            } catch (Exception ex) {
                Exceptions.ignore(ex);
            }
        }
    }

    private void send100Continue(ChannelHandlerContext e) {
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        e.channel().write(response);
    }

    /*
     * Handles POST or PUT request - those are the only methods where we expect uploaded content.
     */
    private boolean handlePOSTandPUT(HttpRequest req, boolean chunked) throws Exception {
        try {
            String contentType = req.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            if (Strings.isFilled(contentType) && (contentType.startsWith("multipart/form-data") || contentType.startsWith(
                    "application/x-www-form-urlencoded"))) {
                if (WebServer.LOG.isFINE()) {
                    WebServer.LOG.FINE("POST/PUT-FORM: " + req.getUri());
                }
                HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(WebServer.getHttpDataFactory(), req);
                currentContext.setPostDecoder(postDecoder);
            } else {
                if (WebServer.LOG.isFINE()) {
                    WebServer.LOG.FINE("POST/PUT-DATA: " + req.getUri());
                }
                Attribute body = WebServer.getHttpDataFactory().createAttribute(req, "body");
                if (req instanceof FullHttpRequest) {
                    body.setContent(((FullHttpRequest) req).content());
                }
                currentContext.content = body;
            }

        } catch (Throwable ex) {
            currentContext.respondWith()
                    .error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, ex));
            return true;
        }
        if (!chunked) {
            dispatch();
        }
        return false;
    }

    /*
     * Handles GET, HEAD or DELETE requests, where we don't expect any content
     */
    private boolean handleGETorHEADorDELETE(HttpRequest req, boolean chunked) throws Exception {
        if (chunked) {
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
    private void continueChunkedRequest(ChannelHandlerContext ctx, Object e) {
        HttpContent chunk = (HttpContent) e;
        try {
            if (currentContext.getPostDecoder() != null) {
                if (WebServer.LOG.isFINE()) {
                    WebServer.LOG
                            .FINE("POST-CHUNK: " + currentContext.getRequestedURI() + " - " + chunk.content()
                                    .readableBytes() + " bytes");
                }
                currentContext.getPostDecoder().offer(chunk);
            } else {
                if (WebServer.LOG.isFINE()) {
                    WebServer.LOG
                            .FINE("DATA-CHUNK: " + currentContext.getRequestedURI() + " - " + chunk.content()
                                    .readableBytes() + " bytes");
                }
                currentContext.content.addContent(chunk.content(), chunk instanceof LastHttpContent);
                if (!currentContext.content.isInMemory()) {
                    File file = currentContext.content.getFile();
                    checkUploadFileLimits(file);
                }
            }
            if (chunk instanceof LastHttpContent) {
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
            if (WebServer.LOG.isFINE()) {
                WebServer.LOG.FINE("Not enough space to handle: " + currentContext.getRequestedURI());
            }
            currentContext.respondWith()
                    .error(HttpResponseStatus.INSUFFICIENT_STORAGE,
                            Exceptions.handle()
                                    .withSystemErrorMessage(
                                            "The web server is running out of temporary space to store the upload")
                                    .to(WebServer.LOG)
                                    .handle());
        }
        if (file.length() > WebServer.getMaxUploadSize() && WebServer.getMaxUploadSize() > 0) {
            if (WebServer.LOG.isFINE()) {
                WebServer.LOG.FINE("Body is too large: " + currentContext.getRequestedURI());
            }
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
        if (WebServer.LOG.isFINE() && currentContext != null) {
            WebServer.LOG.FINE("DISPATCHING: " + currentContext.getRequestedURI());
        }

        for (WebDispatcher wd : sortedDispatchers) {
            try {
                if (wd.dispatch(currentContext)) {
                    if (WebServer.LOG.isFINE()) {
                        WebServer.LOG.FINE("DISPATCHED: " + currentContext.getRequestedURI() + " to " + wd);
                    }
                    currentRequest = null;
                    return;
                }
            } catch (Exception e) {
                Exceptions.handle(WebServer.LOG, e);
            }
        }
    }


}
