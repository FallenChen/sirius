package sirius.web.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.jboss.netty.util.CharsetUtil;
import org.rythmengine.Rythm;
import sirius.kernel.Sirius;
import sirius.kernel.nls.NLS;
import sirius.kernel.commons.Strings;
import sirius.kernel.async.CallContext;
import sirius.kernel.health.Exceptions;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 15.07.13
 * Time: 17:27
 * To change this template use File | Settings | File Templates.
 */
public class Response {
    public static final int HTTP_CACHE = 60 * 60;
    public static final MimetypesFileTypeMap MIME_TYPES_MAP = new MimetypesFileTypeMap();
    private WebContext wc;
    private ChannelHandlerContext ctx;
    private Integer cacheSeconds = null;
    private boolean download = false;
    private boolean isPrivate = false;
    private String name;

    protected Response(WebContext wc) {
        this.wc = wc;
        this.ctx = wc.getCtx();
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

    public Response noCache() {
        this.cacheSeconds = 0;
        return this;
    }

    public Response privateCached() {
        this.isPrivate = true;
        this.cacheSeconds = HTTP_CACHE;
        return this;
    }

    public Response cache() {
        this.isPrivate = false;
        this.cacheSeconds = HTTP_CACHE;
        return this;
    }

    public boolean wasModified(long lastModifiedInMillis) {
        long ifModifiedSinceDateSeconds = wc.getDateHeader(HttpHeaders.Names.IF_MODIFIED_SINCE) / 1000;
        if (ifModifiedSinceDateSeconds > 0 && lastModifiedInMillis > 0) {
            if (ifModifiedSinceDateSeconds >= lastModifiedInMillis / 1000) {
                status(HttpResponseStatus.NOT_MODIFIED);
                return false;
            }
        }

        return true;
    }

    public void status(HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        addKeepAlive(response);
        keepAliveOrClose(ctx.getChannel().write(response));
    }

    public void redirectTemporiarily(HttpResponseStatus status, String url) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT);
        response.setHeader(HttpHeaders.Names.LOCATION, url);
        addKeepAlive(response);
        keepAliveOrClose(ctx.getChannel().write(response));
    }

    public void redirectPermanently(HttpResponseStatus status, String url) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.MOVED_PERMANENTLY);
        response.setHeader(HttpHeaders.Names.LOCATION, url);
        addKeepAlive(response);
        keepAliveOrClose(ctx.getChannel().write(response));
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
        } catch (FileNotFoundException fnfe) {
            WebServer.LOG.FINE(fnfe);
            error(HttpResponseStatus.NOT_FOUND);
            return;
        }
        try {
            long fileLength = raf.length();

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpHeaders.setContentLength(response, fileLength);
            setContentTypeHeader(response, name != null ? name : file.getName());
            setDateAndCacheHeaders(response,
                                   file.lastModified(),
                                   cacheSeconds == null ? HTTP_CACHE : cacheSeconds,
                                   isPrivate);
            if (name != null) {
                setContentDisposition(response, name, download);
            }

            addKeepAlive(response);

            // Write the initial line and the header.
            ctx.getChannel().write(response);

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
            keepAliveOrClose(writeFuture);
        } catch (IOException e) {
            WebServer.LOG.FINE(e);
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     */
    private static void setDateAndCacheHeaders(HttpResponse response,
                                               long lastModifiedMillis,
                                               int cacheSeconds,
                                               boolean isPrivate) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(WebContext.HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(WebContext.HTTP_DATE_GMT_TIMEZONE));

        if (cacheSeconds > 0) {
            // Date header
            Calendar time = new GregorianCalendar();
            response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

            // Add cache headers
            time.add(Calendar.SECOND, cacheSeconds);
            response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
            if (isPrivate) {
                response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + cacheSeconds);
            } else {
                response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=" + cacheSeconds);
            }
        } else {
            response.setHeader(HttpHeaders.Names.CACHE_CONTROL, HttpHeaders.Values.NO_CACHE);
        }
        if (lastModifiedMillis > 0) {
            response.setHeader(HttpHeaders.Names.
                                       LAST_MODIFIED, dateFormatter.format(new Date(lastModifiedMillis)));
        }
    }

    /**
     * Sets the content disposition header for the HTTP Response
     */
    private static void setContentDisposition(HttpResponse response, String name, boolean download) {
        response.setHeader("Content-Disposition",
                           download ? "attachment;" : "inline;" + "filename=\"" + name.replaceAll("[^A-Za-z0-9\\-_\\.]",
                                                                                                  "_") + "\"");
    }

    /**
     * Sets the content type header for the HTTP Response
     */
    private static void setContentTypeHeader(HttpResponse response, String name) {
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, MIME_TYPES_MAP.getContentType(name));
    }

    public void resource(URLConnection urlConnection) {
        try {
            long fileLength = urlConnection.getContentLength();

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpHeaders.setContentLength(response, fileLength);
            setContentTypeHeader(response, name != null ? name : urlConnection.getURL().getFile());
            setDateAndCacheHeaders(response,
                                   urlConnection.getLastModified(),
                                   cacheSeconds == null ? HTTP_CACHE : cacheSeconds,
                                   isPrivate);
            if (name != null) {
                setContentDisposition(response, name, download);
            }
            addKeepAlive(response);

            // Write the initial line and the header.
            ctx.getChannel().write(response);

            // Write the content.
            ChannelFuture writeFuture = ctx.getChannel().write(new ChunkedStream(urlConnection.getInputStream(), 8192));
            keepAliveOrClose(writeFuture);
        } catch (IOException e) {
            WebServer.LOG.FINE(e);
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void error(HttpResponseStatus status) {
        error(status, "");
    }

    public void error(HttpResponseStatus status, Throwable t) {
        error(status, NLS.toUserString(t));
    }

    public void error(HttpResponseStatus status, String message) {
        try {
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            String content = Rythm.renderIfTemplateExists("view/errors/" + status.getCode() + ".html",
                                                          CallContext.getCurrent(),
                                                          status,
                                                          message);
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists("view/errors/error.html",
                                                       CallContext.getCurrent(),
                                                       status,
                                                       message);
            }
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists("view/errors/default.html",
                                                       CallContext.getCurrent(),
                                                       status,
                                                       message);
            }
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
            response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            HttpHeaders.setContentLength(response, channelBuffer.capacity());
            response.setContent(channelBuffer);
            ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
        } catch (Throwable e) {
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                            HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(Exceptions.handle(WebServer.LOG, e).getMessage(),
                                                                      CharsetUtil.UTF_8);
            HttpHeaders.setContentLength(response, channelBuffer.capacity());
            response.setContent(channelBuffer);
            ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void template(String name, Object... params) {
        try {
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            String content = Rythm.render(name, params);
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            if (name.endsWith("html")) {
                response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            } else {
                setContentTypeHeader(response, name);
            }
            setDateAndCacheHeaders(response,
                                   System.currentTimeMillis(),
                                   cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                   isPrivate);
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            HttpHeaders.setContentLength(response, channelBuffer.capacity());
            response.setContent(channelBuffer);
            keepAliveOrClose(ctx.getChannel().write(response));
        } catch (Throwable e) {
            //TODO log + besser machen!
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private void addKeepAlive(HttpResponse response) {
        if (HttpHeaders.isKeepAlive(wc.getRequest())) {
            response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
    }


    private void keepAliveOrClose(ChannelFuture future) {
        if (!HttpHeaders.isKeepAlive(wc.getRequest())) {
            // Close the connection when the whole content is written out.
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void nlsTemplate(String name, Object... params) {
        try {
            if (!ctx.getChannel().isWritable()) {
                return;
            }
            String content = Rythm.renderIfTemplateExists(name + "_" + NLS.getCurrentLang() + ".html", params);
            if (Strings.isEmpty(content)) {
                content = Rythm.renderIfTemplateExists(name + "_" + NLS.getDefaultLanguage() + ".html", params);
            }
            if (Strings.isEmpty(content)) {
                content = Rythm.render(name + ".html", params);
            }
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            addKeepAlive(response);
            setDateAndCacheHeaders(response,
                                   System.currentTimeMillis(),
                                   cacheSeconds == null || Sirius.isDev() ? 0 : cacheSeconds,
                                   isPrivate);
            ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(content, CharsetUtil.UTF_8);
            HttpHeaders.setContentLength(response, channelBuffer.capacity());
            response.setContent(channelBuffer);
            keepAliveOrClose(ctx.getChannel().write(response));
        } catch (Throwable e) {
            //TODO log + besser machen!
            error(HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

}
