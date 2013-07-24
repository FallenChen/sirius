/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.commons.Callback;
import sirius.kernel.commons.ValueHolder;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.07.13
 * Time: 23:39
 * To change this template use File | Settings | File Templates.
 */
public class Promise<V> {

    private ValueHolder<V> value;
    private Throwable failure;
    private List<CompletionHandler<V>> handlers = new ArrayList<CompletionHandler<V>>(1);

    public V get() {
        return value != null ? value.get() : null;
    }

    public void success(final V value) {
        this.value = new ValueHolder<V>(value);
        for (final CompletionHandler<V> handler : handlers) {
            completeHandler(value, handler);
        }
    }

    private void completeHandler(final V value, final CompletionHandler<V> handler) {
        try {
            handler.onSuccess(value);
        } catch (Throwable e) {
            Exceptions.handle(Async.LOG, e);
        }
    }

    public void fail(final Throwable exception) {
        this.failure = exception;
        for (final CompletionHandler<V> handler : handlers) {
            failHandler(exception, handler);
        }
    }

    private void failHandler(final Throwable exception, final CompletionHandler<V> handler) {
        try {
            handler.onFailure(exception);
        } catch (Throwable e) {
            Exceptions.handle(Async.LOG, e);
        }
    }

    public boolean isCompleted() {
        return isFailed() || isSuccessful();
    }

    public boolean isFailed() {
        return failure != null;
    }

    public boolean isSuccessful() {
        return value != null;
    }

    public Throwable getFailure() {
        return failure;
    }

    public <X> Promise<X> map(final Mapper<V, X> mapper) {
        final Promise<X> result = new Promise<X>();
        onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                try {
                    result.success(mapper.apply(value));
                } catch (Throwable throwable) {
                    result.fail(throwable);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                result.fail(throwable);
            }
        });

        return result;
    }

    public <X> Promise<X> flatMap(final Mapper<V, Promise<X>> mapper) {
        final Promise<X> result = new Promise<X>();
        onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                try {
                    mapper.apply(value).chain(result);
                } catch (Throwable throwable) {
                    result.fail(throwable);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                result.fail(throwable);
            }
        });

        return result;
    }

    public void chain(final Promise<V> promise) {
        onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                promise.success(value);
            }

            @Override
            public void onFailure(Throwable throwable) {
                promise.fail(throwable);
            }
        });
    }


    public <X> void mapChain(final Promise<X> promise, final Mapper<V, X> mapper) {
        onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                try {
                    promise.success(mapper.apply(value));
                } catch (Throwable e) {
                    promise.fail(e);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                promise.fail(throwable);
            }
        });
    }

    public <X> Promise<V> failChain(final Promise<X> promise, final Callback<V> successHandler) {
        return onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                try {
                    successHandler.invoke(value);
                } catch (Throwable e) {
                    promise.fail(e);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                promise.fail(throwable);
            }
        });
    }

    public Promise<V> onComplete(CompletionHandler<V> handler) {
        if (handler != null) {
            if (isSuccessful()) {
                completeHandler(get(), handler);
            } else if (isFailed()) {
                failHandler(getFailure(), handler);
            } else {
                this.handlers.add(handler);
            }
        }

        return this;
    }

    public Promise<V> onSuccess(final Callback<V> successHandler) {
        return onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
                try {
                    successHandler.invoke(value);
                } catch (Throwable t) {
                    fail(t);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
    }

    public Promise<V> onFailure(final Callback<Throwable> failureHandler) {
        return onComplete(new CompletionHandler<V>() {
            @Override
            public void onSuccess(V value) throws Exception {
            }

            @Override
            public void onFailure(Throwable throwable) throws Exception {
                failureHandler.invoke(throwable);
            }
        });
    }

    public Promise<V> traceErrors(Log logger, Callback<Throwable> error) {
        if (logger.isFINE()) {
            return onFailure(error);
        }

        return this;
    }

    public Promise<V> logErrors() {
        return logErrors(Async.LOG);
    }

    public Promise<V> logErrors(Log log) {
        return onFailure(new Callback<Throwable>() {
            @Override
            public void invoke(Throwable value) throws Exception {
                Exceptions.handle(Async.LOG, value);
            }
        });
    }
}
