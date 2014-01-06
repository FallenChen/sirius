package sirius.app.servlet;

import com.google.common.collect.Lists;
import sirius.kernel.async.Async;
import sirius.kernel.health.Exceptions;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 05.01.14
 * Time: 13:17
 * To change this template use File | Settings | File Templates.
 */
public class AsyncContextAdapter implements AsyncContext {
    protected final RequestAdapter origRequest;
    protected final ResponseAdapter origResponse;
    protected final ServletRequest servletRequest;
    protected final ServletResponse servletResponse;
    protected final List<AsyncListener> listeners = Lists.newCopyOnWriteArrayList();

    public AsyncContextAdapter(RequestAdapter origRequest, ResponseAdapter origResponse, ServletRequest servletRequest, ServletResponse servletResponse) {
        this.origRequest = origRequest;
        this.origResponse = origResponse;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    @Override
    public ServletRequest getRequest() {
        return servletRequest;
    }

    @Override
    public ServletResponse getResponse() {
        return servletResponse;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return origRequest == servletRequest && origResponse == servletResponse;
    }

    @Override
    public void dispatch() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispatch(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void complete() {
        for (AsyncListener listener : listeners) {
            try {
                listener.onComplete(new AsyncEvent(this, servletRequest, servletResponse));
            } catch (IOException e) {
                Exceptions.handle(e);
            }
        }
        origResponse.complete();
    }

    @Override
    public void start(Runnable run) {
        Async.executor("servlet-async").fork(run).execute();
    }

    @Override
    public void addListener(AsyncListener listener) {
        listeners.add(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeout(long timeout) {
        origRequest.ctx.markAsLongCall();
    }

    @Override
    public long getTimeout() {
        return 1000;
    }
}
