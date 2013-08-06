package sirius.app.servlet;

import com.google.common.collect.Lists;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;
import sirius.kernel.async.CallContext;
import sirius.kernel.commons.MultiMap;
import sirius.kernel.commons.Strings;
import sirius.kernel.commons.ValueHolder;
import sirius.web.http.MimeHelper;
import sirius.web.http.Response;
import sirius.web.http.WebContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.07.13¡
 * Time: 17:54
 * To change this template use File | Settings | File Templates.
 */
public class ResponseAdapter implements HttpServletResponse {

    private WebContext wc;
    private PrintWriter writer;
    private ServletOutputStream stream;
    private String characterEncoding = null;
    private List<Cookie> cookies = Lists.newArrayList();
    private MultiMap<String, Object> headers = MultiMap.create();
    private int status = SC_OK;
    private Integer contentLength;
    private String contentType;
    private boolean outputWritten = false;
    private ValueHolder<ChannelFuture> future = new ValueHolder<ChannelFuture>(null);

    public ResponseAdapter(WebContext ctx) {
        this.wc = ctx;
    }


    @Override
    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String s) {
        return headers.keySet().contains(s);
    }

    @Override
    public String encodeURL(String s) {
        return Strings.urlEncode(s);
    }

    @Override
    public String encodeRedirectURL(String s) {
        return Strings.urlEncode(s);
    }

    @Override
    @Deprecated
    public String encodeUrl(String s) {
        return Strings.urlEncode(s);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String s) {
        return Strings.urlEncode(s);
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        assertNotCommitted();
        outputWritten = true;
        wc.respondWith().error(HttpResponseStatus.valueOf(i), s);
    }

    private void assertNotCommitted() {
        if (isCommitted()) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void sendError(int i) throws IOException {
        assertNotCommitted();
        outputWritten = true;
        wc.respondWith().error(HttpResponseStatus.valueOf(i));
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        sendError(SC_TEMPORARY_REDIRECT, s);
    }

    @Override
    public void setDateHeader(String s, long l) {
        headers.set(s, l);
    }

    @Override
    public void addDateHeader(String s, long l) {
        headers.put(s, l);
    }

    @Override
    public void setHeader(String s, String s2) {
        headers.set(s, s2);
    }

    @Override
    public void addHeader(String s, String s2) {
        headers.put(s, s2);
    }

    @Override
    public void setIntHeader(String s, int i) {
        headers.set(s, i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        headers.put(s, i);
    }

    @Override
    public void setStatus(int i) {
        this.status = i;
    }

    @Override
    @Deprecated
    public void setStatus(int i, String s) {
        this.status = i;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (stream != null) {
            return stream;
        }
        if (outputWritten) {
            throw new IllegalStateException();
        }
        stream = new ServletOutputStream() {

            private OutputStream os;

            private void makeOutputStream() {
                outputWritten = true;
                String content = contentType;
                if (Strings.isEmpty(content)) {
                    content = MimeHelper.guessMimeType(wc.getRequestedURI());
                } else {
                    if (characterEncoding != null && !contentType.contains(";")) {
                        content = contentType + ";charset=" + characterEncoding;
                    }
                }

                for (Cookie cookie : cookies) {
                    DefaultCookie nettyCookie = new DefaultCookie(cookie.getName(), cookie.getValue());
                    nettyCookie.setComment(cookie.getComment());
                    nettyCookie.setDomain(cookie.getDomain());
                    nettyCookie.setMaxAge(cookie.getMaxAge());
                    nettyCookie.setPath(cookie.getPath());
                    nettyCookie.setSecure(cookie.getSecure());
                    nettyCookie.setVersion(cookie.getVersion());
                    wc.setCookie(nettyCookie);
                }

                os = wc.respondWith()
                       .headers(headers)
                       .outputStream(HttpResponseStatus.valueOf(status), content, contentLength);
            }

            @Override
            public void write(int b) throws IOException {
                if (os == null) {
                    makeOutputStream();
                }
                os.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                if (os == null) {
                    makeOutputStream();
                }
                os.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (os == null) {
                    makeOutputStream();
                }
                os.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                if (os == null) {
                    makeOutputStream();
                }
                os.flush();
            }

            @Override
            public void close() throws IOException {
                if (os == null) {
                    makeOutputStream();
                }
                os.close();
            }
        };

        return stream;
    }

    public void complete() {
        try {
            if (writer != null) {
                writer.flush();
            } else if (stream == null) {
                getOutputStream();
            }
            stream.close();
        } catch (IOException e) {
            ServletContainer.LOG.FINE(e);
            if (wc.getCtx().getChannel().isOpen()) {
                wc.getCtx().getChannel().close();
            }
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            if (isCommitted()) {
                throw new IllegalStateException();
            }
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(),
                                                            characterEncoding == null ? CharsetUtil.UTF_8
                                                                                                   .name() : characterEncoding));
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String s) {
        characterEncoding = s;
    }

    @Override
    public void setContentLength(int i) {
        contentLength = i;
    }

    @Override
    public void setContentType(String s) {
        assertNotCommitted();
        this.contentType = s;
    }

    @Override
    public void setBufferSize(int i) {

    }

    @Override
    public int getBufferSize() {
        return Response.BUFFER_SIZE;
    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        return outputWritten;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale locale) {
        CallContext.getCurrent().setLang(locale.getLanguage());
    }

    @Override
    public Locale getLocale() {
        return new Locale(CallContext.getCurrent().getLang());
    }
}
