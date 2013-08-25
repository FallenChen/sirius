package sirius.web.http;


import com.google.common.base.Charsets;
import com.ning.http.client.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.jboss.netty.util.CharsetUtil;
import org.rythmengine.Rythm;
import sirius.kernel.Sirius;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.HandledException;
import sirius.kernel.nls.NLS;
import sirius.kernel.xml.StructuredOutput;
import sirius.web.services.JSONStructuredOutput;
import sirius.web.templates.RythmConfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.channels.ClosedChannelException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 15.07.13
 * Time: 17:27
 * To change this template use File | Settings | File Templates.
 */
public class Response {
    public static final int HTTP_CACHE = 60 * 60;
    public static final int HTTP_CACHE_INIFINITE = 60 * 60 * 24 * 356 * 20;
    public static final int BUFFER_SIZE = 8192;

    private WebContext wc;
    private ChannelHandlerContext ctx;
    private MultiMap<String, Object> headers;
    private Integer cacheSeconds = null;
    private boolean download = false;
    private boolean isPrivate = false;
    private String name;
    private String timing = null;
    private SimpleDateFormat dateFormatter;

    protected Response(WebContext wc) {
        this.wc = wc;
        this.ctx = wc.getCtx();
    }

    private HttpResponse createResponse(HttpResponseStatus status, boolean keepalive) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);

        //Apply headers
        if (headers != null) {
            for (Map.Entry<String, Collection<Object>> e : headers.getUnderlyingMap().entrySet()) {
                for (Object value : e.getValue()) {
                    response.addHeader(e.getKey(), value);
                }
            }
        }

        // Add keepalive header is required
        if (keepalive && isKeepalive()) {
            response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Add cookies
        Collection<Cookie> cookies = wc.getOutCookies();
        if (cookies != null && !cookies.isEmpty()) {
            CookieEncoder ce = new CookieEncoder(false);
            for (Cookie c : cookies) {
                ce.addCookie(c);
            }
            response.addHeader(HttpHeaders.Names.SET_COOKIE, ce.encode());
        }

        // Add Server: nodeName as header
        response.addHeader(HttpHeaders.Names.SERVER, CallContext.getCurrent().getNodeName());

        // Add a P3P-Header. This is used to disable the 3rd-Party auth handling of InternetExplorer
        // which is pretty broken and not used (google and facebook does the same).
        if (wc.addP3PHeader) {
            response.setHeader("P3P", "CP=\"This site does not have a p3p policy.\"");
        }
        return response;
    }

    private ChannelFuture commit(HttpResponse response) {
        if (wc.responseCommitted) {
            throw Exceptions.handle()
                            .to(WebServer.LOG)
                            .error(new IllegalStateException())
                            .withSystemErrorMessage("Response for %s was already committed!", wc.getRequestedURI())
                            .handle();
        }
        wc.responseCommitted = true;
        return ctx.getChannel().write(response);
    }

    private boolean isKeepalive() {
        return HttpHeaders.isKeepAlive(wc.getRequest()) && ((WebServerHandler) ctx.getHandler()).shouldKeepAlive();
    }

    private void complete(ChannelFuture future, final boolean keepalive) {
        final CallContext cc = CallContext.getCurrent();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (wc.completionCallback != null) {
                    try {
                        wc.completionCallback.invoke(cc);
                    } catch (Throwable e) {
                        Exceptions.handle(WebServer.LOG, e);
                    }
                }
                if (timing != null) {
                    cc.getWatch().submitMicroTiming(timing);
                }
                if (!keepalive || !isKeepalive()) {
                    future.getChannel().close();
                }
            }
        });
    }

    private void complete(ChannelFuture future) {
        complete(future, true);
    }

    private void completeAndClose(ChannelFuture future) {
        complete(future, false);
    }

    private boolean wasModified(long lastModifiedInMillis) {
        long ifModifiedSinceDateSeconds = wc.getDateHeader(HttpHeaders.Names.IF_MODIFIED_SINCE) / 1000;
        if (ifModifiedSinceDateSeconds > 0 && lastModifiedInMillis > 0) {
            if (ifModifiedSinceDateSeconds >= lastModifiedInMillis / 1000) {
                status(HttpResponseStatus.NOT_MODIFIED);
                return false;
            }
        }

        return true;
    }

    public Response download(String name) {
        this.name = name;
        this.download = true;
        return this;
    }

    public Response inline(String name) {
        this.name = name;
        this.download = false;
        return this;
    }

    public Response notCached() {
        this.cacheSeconds = 0;
        return this;
    }

    public Response privateCached() {
        this.isPrivate = true;
        this.cacheSeconds = HTTP_CACHE;
        return this;
    }

    public Response cached() {
        this.isPrivate = false;
        this.cacheSeconds = HTTP_CACHE;
        return this;
    }

    public Response infinitelyCached() {
        this.isPrivate = false;
        this.cacheSeconds = HTTP_CACHE_INIFINITE;
        return this;
    }

    public Response setHeader(String name, Object value) {
        if (headers == null) {
            headers = MultiMap.create();
        }
        headers.set(name, value);
        return this;
    }

    public Response addHeader(String name, Object value) {
        if (headers == null) {
            headers = MultiMap.create();
        }
        headers.put(name, value);
        return this;
    }

    public Response addHeaderIfNotExists(String name, Object value) {
        if (headers == null) {
            headers = MultiMap.create();
        }
        if (!headers.keySet().contains(name)) {
            headers.put(name, value);
        }
        return this;
    }


    public Response headers(MultiMap<String, Object> inputHeaders) {
        for (Map.Entry<String, Collection<Object>> e : inputHeaders.getUnderlyingMap().entrySet()) {
            for (Object value : e.getValue()) {
                addHeader(e.getKey(), value);
            }
        }
        return this;
    }


    public void status(HttpResponseStatus status) {
        HttpResponse response = createResponse(status, true);
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, 0);
        complete(commit(response));
    }


    public void redirectTemporarily(String url) {
        HttpResponse response = createResponse(HttpResponseStatus.TEMPORARY_REDIRECT, true);
        response.setHeader(HttpHeaders.Names.LOCATION, url);
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, 0);
        complete(commit(response));
    }

    public void redirectPermanently(String url) {
        HttpResponse response = createResponse(HttpResponseStatus.MOVED_PERMANENTLY, true);
        response.setHeader(HttpHeaders.Names.LOCATION, url);
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, 0);
        complete(commit(response));
    }

    public void file(File file) {
        if (file.isHidden() || !file.exists()) {
            error(HttpResponseStatus.NOT_FOUND);
            return;
        }


        if (!file.isFile()) {
            error(HttpResponseStatus.FORBIDDEN);
            return;
        }

        if (!wasModified(file.lastModified())) {
            return;
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (IOException io) {
            WebServer.LOG.FINE(io);
            error(HttpResponseStatus.NOT_FOUND);
            return;
        }
        try {
            long fileLength = raf.length();

            addHeaderIfNotExists(HttpHeaders.Names.CONTENT_LENGTH, fileLength);
            setContentTypeHeader(name != null ? name : file.getName());
            setDateAndCacheHeaders(file.lastModified(), cacheSeconds == null ? HTTP_CACHE : cacheSeconds, isPrivate);
            if (name != null) {
                setContentDisposition(name, download);
            }
            HttpResponse response = createResponse(HttpResponseStatus.OK, true);

            commit(response);

            // Write the content.
            ChannelFuture writeFuture;
            if (ctx.getChannel().getPipeline().get(SslHandler.class) != null) {
                // Cannot use zero-copy with HTTPS.
                writeFuture = ctx.getChannel().write(new ChunkedFile(raf, 0, fileLength, 8192));
            } else {
                // No encryption - use zero-copy.
                final FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
                writeFuture = ctx.getChannel().write(region);
            }
            complete(writeFuture);
        } catch (Throwable e) {
            internalServerError(e);
        }
    }

    private void internalServerError(Throwable t) {
        WebServer.LOG.FINE(t);
        if (!(t instanceof ClosedChannelException)) {
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.createHandled().error(t).handle());
        }
        if (!ctx.getChannel().isOpen()) {
            ctx.getChannel().close();
        }
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     */
    private void setDateAndCacheHeaders(long lastModifiedMillis, int cacheSeconds, boolean isPrivate) {
        Set<String> keySet = null;
        if (headers != null) {
            keySet = headers.keySet();
            if (keySet.contains(HttpHeaders.Names.EXPIRES) || keySet.contains(HttpHeaders.Names.CACHE_CONTROL)) {
                return;
            }
        }
        SimpleDateFormat dateFormatter = getHTTPDateFormat();

        if (cacheSeconds > 0) {
            // Date header
            Calendar time = new GregorianCalendar();
            addHeaderIfNotExists(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

            // Add cached headers
            time.add(Calendar.SECOND, cacheSeconds);
            addHeaderIfNotExists(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
            if (isPrivate) {
                addHeaderIfNotExists(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + cacheSeconds);
            } else {
                addHeaderIfNotExists(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=" + cacheSeconds);
            }
        } else {
            addHeaderIfNotExists(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
        }
        if (lastModifiedMillis > 0 && (keySet == null || !keySet.contains(HttpHeaders.Names.LAST_MODIFIED))) {
            addHeaderIfNotExists(HttpHeaders.Names.
                                         LAST_MODIFIED, dateFormatter.format(new Date(lastModifiedMillis)));
        }
    }

    private SimpleDateFormat getHTTPDateFormat() {
        if (dateFormatter == null) {
            dateFormatter = new SimpleDateFormat(WebContext.HTTP_DATE_FORMAT, Locale.US);
            dateFormatter.setTimeZone(TimeZone.getTimeZone(WebContext.HTTP_DATE_GMT_TIMEZONE));
        }
        return dateFormatter;
    }

    /**
     * Sets the content disposition header for the HTTP Response
     */
    private void setContentDisposition(String name, boolean download) {
        addHeaderIfNotExists("Content-Disposition",
                             download ? "attachment;" : "inline;" + "filename=\"" + name.replaceAll(
                                     "[^A-Za-z0-9\\-_\\.]",
                                     "_") + "\"");
    }

    /**
     * Sets the content type header for the HTTP Response
     */
    private void setContentTypeHeader(String name) {
        addHeaderIfNotExists(HttpHeaders.Names.CONTENT_TYPE, MimeHelper.guessMimeType(name));
    }

    public void resource(URLConnection urlConnection) {
        HttpResponse response = null;
        try {
            long fileLength = urlConnection.getContentLength();

            HttpHeaders.setContentLength(response, fileLength);
            setContentTypeHeader(name != null ? name : urlConnection.getURL().getFile());
            setDateAndCacheHeaders(urlConnection.getLastModified(),
                                   cacheSeconds == null ? HTTP_CACHE : cacheSeconds,
                                   isPrivate);
            if (name != null) {
                setContentDisposition(name, download);
            }
            response = createResponse(HttpResponseStatus.OK, true);
        } catch (Throwable t) {
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, t));
            return;
        }
        try {
            commit(response);

            // Write the initial line and the header.
            ctx.getChannel().write(response);

            // Write the content.
            ChannelFuture writeFuture = ctx.getChannel().write(new ChunkedStream(urlConnection.getInputStream(), 8192));
            complete(writeFuture);
        } catch (Throwable e) {
            internalServerError(e);
        }
    }

    public void error(HttpResponseStatus status) {
        error(status, "");
    }

    public void error(HttpResponseStatus status, HandledException t) {
        error(status, NLS.toUserString(t));
    }

    public void error(HttpResponseStatus status, String message) {
        try {
            if (wc.responseCommitted) {
                if (ctx.getChannel().isOpen()) {
                    ctx.getChannel().close();
                }
                return;
            }
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            String content = Rythm.renderIfTemplateExists("view/errors/" + status.getCode() + ".html", status, message);
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists("view/errors/error.html", status, message);
            }
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists("view/errors/default.html", status, message);
            }
            setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            setHeader(HttpHeaders.Names.CONTENT_LENGTH, channelBuffer.readableBytes());
            HttpResponse response = createResponse(status, false);
            commit(response);
            completeAndClose(ctx.getChannel().write(channelBuffer));
        } catch (Throwable e) {
            WebServer.LOG.FINE(e);
            if (wc.responseCommitted) {
                if (ctx.getChannel().isOpen()) {
                    ctx.getChannel().close();
                }
                return;
            }
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            HttpResponse response = createResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, false);
            response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(Exceptions.handle(WebServer.LOG, e).getMessage(),
                                                                      CharsetUtil.UTF_8);
            HttpHeaders.setContentLength(response, channelBuffer.readableBytes());
            commit(response);
            completeAndClose(ctx.getChannel().write(channelBuffer));
        }
    }

    public void direct(HttpResponseStatus status, String content) {
        try {
            setDateAndCacheHeaders(System.currentTimeMillis(),
                                   cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                   isPrivate);
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            setHeader(HttpHeaders.Names.CONTENT_LENGTH, channelBuffer.readableBytes());
            HttpResponse response = createResponse(status, true);
            commit(response);
            complete(ctx.getChannel().write(channelBuffer));
        } catch (Throwable e) {
            internalServerError(e);
        }
    }

    public void template(String name, Object... params) {
        String content = null;
        try {
            if (params.length == 1 && params[0] instanceof Object[]) {
                params = (Object[]) params[0];
            }
            content = Rythm.render(name, params);
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(RythmConfig.LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to render the template '%s': %s (%s)", name)
                            .handle();
        }
        try {
            if (name.endsWith("html")) {
                setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            } else {
                setContentTypeHeader(name);
            }
            setDateAndCacheHeaders(System.currentTimeMillis(),
                                   cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                   isPrivate);
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            setHeader(HttpHeaders.Names.CONTENT_LENGTH, channelBuffer.readableBytes());
            HttpResponse response = createResponse(HttpResponseStatus.OK, true);
            commit(response);
            complete(ctx.getChannel().write(channelBuffer));
        } catch (Throwable e) {
            internalServerError(e);
        }
    }

    public void tunnel(final String url) {
        try {
            AsyncHttpClient c = new AsyncHttpClient();
            AsyncHttpClient.BoundRequestBuilder brb = c.prepareGet(url);

            // Support caching...
            long ifModifiedSince = wc.getDateHeader(HttpHeaders.Names.IF_MODIFIED_SINCE);
            if (ifModifiedSince > 0) {
                brb.addHeader(HttpHeaders.Names.IF_MODIFIED_SINCE, getHTTPDateFormat().format(ifModifiedSince));
            }

            // Support range requests...
            String range = wc.getHeader(HttpHeaders.Names.RANGE);
            if (Strings.isFilled(range)) {
                brb.addHeader(HttpHeaders.Names.RANGE, range);
            }

            // Tunnel it through...
            brb.execute(new AsyncHandler<String>() {

                ChannelFuture lastFuture;
                boolean isChucked;
                boolean contentLengthFound = false;

                @Override
                public AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders h) throws Exception {
                    FluentCaseInsensitiveStringsMap headers = h.getHeaders();

                    boolean contentTypeFound = false;
                    long lastModified = 0;

                    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        if (!entry.getKey().startsWith("x-") && !entry.getKey().equals("Server")) {
                            for (String value : entry.getValue()) {
                                setHeader(entry.getKey(), value);
                                if (HttpHeaders.Names.LAST_MODIFIED.equals(entry.getKey())) {
                                    try {
                                        lastModified = getHTTPDateFormat().parse(value).getTime();
                                    } catch (Throwable e) {
                                        Exceptions.ignore(e);
                                    }
                                } else if (HttpHeaders.Names
                                        .TRANSFER_ENCODING
                                        .equals(entry.getKey()) && "chunked".equals(value)) {
                                    isChucked = true;
                                }
                            }
                            if (HttpHeaders.Names.CONTENT_TYPE.equals(entry.getKey())) {
                                contentTypeFound = true;
                            } else if (HttpHeaders.Names.CONTENT_LENGTH.equals(entry.getKey())) {
                                contentLengthFound = true;
                            }
                        }
                    }

                    if (!wasModified(lastModified)) {
                        return STATE.ABORT;
                    }

                    if (!contentTypeFound) {
                        setContentTypeHeader(name != null ? name : url);
                    }
                    setDateAndCacheHeaders(lastModified, cacheSeconds == null ? HTTP_CACHE : cacheSeconds, isPrivate);
                    if (name != null) {
                        setContentDisposition(name, download);
                    }

                    HttpResponse response = createResponse(HttpResponseStatus.OK, true);
                    lastFuture = commit(response);

                    return STATE.CONTINUE;
                }

                @Override
                public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                    if (isChucked) {
                        if (bodyPart.isLast()) {
                            ctx.getChannel()
                               .write(new DefaultHttpChunk(ChannelBuffers.copiedBuffer(bodyPart.getBodyByteBuffer())));
                            lastFuture = ctx.getChannel().write(new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER));
                        } else {
                            lastFuture = ctx.getChannel()
                                            .write(new DefaultHttpChunk(ChannelBuffers.copiedBuffer(bodyPart.getBodyByteBuffer())));
                        }
                    } else {
                        lastFuture = ctx.getChannel().write(ChannelBuffers.copiedBuffer(bodyPart.getBodyByteBuffer()));
                    }
                    return STATE.CONTINUE;
                }

                @Override
                public STATE onStatusReceived(com.ning.http.client.HttpResponseStatus httpResponseStatus) throws Exception {
                    if (httpResponseStatus.getStatusCode() == HttpResponseStatus.OK.getCode()) {
                        return STATE.CONTINUE;
                    }
                    if (httpResponseStatus.getStatusCode() == HttpResponseStatus.NOT_MODIFIED.getCode()) {
                        status(HttpResponseStatus.NOT_MODIFIED);
                        return STATE.ABORT;
                    }
                    error(HttpResponseStatus.valueOf(httpResponseStatus.getStatusCode()));
                    return STATE.ABORT;
                }

                @Override
                public String onCompleted() throws Exception {
                    if (!wc.responseCommitted) {
                        if (lastFuture != null) {
                            complete(lastFuture, isChucked || contentLengthFound);
                        } else {
                            error(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    return "";
                }

                @Override
                public void onThrowable(Throwable t) {
                    if (!(t instanceof ClosedChannelException)) {
                        error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, t));
                    }
                }
            });
        } catch (Throwable t) {
            if (!(t instanceof ClosedChannelException)) {
                error(HttpResponseStatus.INTERNAL_SERVER_ERROR, Exceptions.handle(WebServer.LOG, t));
            }
        }
    }


    public void nlsTemplate(String name, Object... params) {
        String content = null;
        try {
            if (params.length == 1 && params[0] instanceof Object[]) {
                params = (Object[]) params[0];
            }
            content = Rythm.renderIfTemplateExists(name + "_" + NLS.getCurrentLang() + ".html", params);
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists(name + "_" + NLS.getDefaultLanguage() + ".html", params);
            }
            if (Strings.isEmpty(content)) {
                content = Rythm.render(name + ".html", params);
            }
        } catch (Throwable e) {
            throw Exceptions.handle()
                            .to(RythmConfig.LOG)
                            .error(e)
                            .withSystemErrorMessage("Failed to render the template '%s': %s (%s)", name)
                            .handle();
        }
        try {
            setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            setDateAndCacheHeaders(System.currentTimeMillis(),
                                   cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                   isPrivate);
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            setHeader(HttpHeaders.Names.CONTENT_LENGTH, channelBuffer.readableBytes());
            HttpResponse response = createResponse(HttpResponseStatus.OK, true);
            commit(response);
            complete(ctx.getChannel().write(channelBuffer));
        } catch (Throwable e) {
            internalServerError(e);
        }
    }

    public StructuredOutput json() {
        String callback = wc.get("callback").getString();
        String encoding = wc.get("encoding")
                            .asString(Strings.isEmpty(callback) ? Charsets.UTF_8.name() : Charsets.ISO_8859_1.name());
        return new JSONStructuredOutput(outputStream(HttpResponseStatus.OK,
                                                     "application/json;charset=" + encoding,
                                                     null), callback, encoding);
    }

    public OutputStream outputStream(final HttpResponseStatus status,
                                     final String contentType,
                                     final Integer contentLength) {
        return new OutputStream() {
            volatile boolean open = true;
            volatile long bytesWritten = 0;
            ChannelBuffer buffer = null;

            private void ensureCapacity(int length) throws IOException {
                if (buffer == null) {
                    buffer = ChannelBuffers.buffer(BUFFER_SIZE);
                }
                if (buffer.writableBytes() < length) {
                    flushBuffer(false);
                }
            }

            private void waitAndClearBuffer(ChannelFuture cf) throws IOException {
                cf.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        buffer.discardReadBytes();
                    }
                });
                try {
                    cf.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    open = false;
                    ctx.getChannel().close();
                    throw new IOException("Interrupted while waiting for a chunk to be written", e);
                }
            }

            private void flushBuffer(boolean last) throws IOException {
                if (wc.responseCommitted) {
                    if (last) {
                        ctx.getChannel().write(new DefaultHttpChunk(buffer));
                        complete(ctx.getChannel().write(new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER)));
                    } else {
                        waitAndClearBuffer(ctx.getChannel().write(new DefaultHttpChunk(buffer)));
                    }
                } else {
                    if (Strings.isFilled(contentType)) {
                        addHeaderIfNotExists(HttpHeaders.Names.CONTENT_TYPE, contentType);
                    }
                    setDateAndCacheHeaders(System.currentTimeMillis(),
                                           cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                           isPrivate);
                    HttpResponse response = createResponse(status, true);
                    if (last) {
                        if (buffer == null) {
                            HttpHeaders.setContentLength(response, 0);
                            complete(commit(response));
                        } else {
                            HttpHeaders.setContentLength(response, buffer.readableBytes());
                            commit(response);
                            complete(ctx.getChannel().write(buffer));
                        }
                    } else {
                        if (contentLength != null) {
                            HttpHeaders.setContentLength(response, contentLength);
                        }
                        response.setChunked(true);
                        commit(response);
                        waitAndClearBuffer(ctx.getChannel().write(new DefaultHttpChunk(buffer)));
                    }
                }
            }

            @Override
            public void write(int b) throws IOException {
                if (!open) {
                    return;
                }
                bytesWritten++;
                if (contentLength == null || bytesWritten < contentLength) {
                    ensureCapacity(1);
                    buffer.writeByte(b);
                }
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (!open) {
                    return;
                }
                if (contentLength != null && bytesWritten + len > contentLength) {
                    len = (int) (contentLength - bytesWritten);
                }
                if (len <= 0) {
                    return;
                }
                // If the given array is larger than out buffer, we repeatedly call write and limit the length to
                // our buffer size.
                if (len > BUFFER_SIZE) {
                    write(b, off, BUFFER_SIZE);
                    write(b, off + BUFFER_SIZE, len - BUFFER_SIZE);
                    return;
                }
                ensureCapacity(len);
                buffer.writeBytes(b, off, len);
            }

            @Override
            public void close() throws IOException {
                if (!open) {
                    return;
                }
                open = false;
                super.close();
                flushBuffer(true);
            }
        };
    }


}
