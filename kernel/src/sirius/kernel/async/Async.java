/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import sirius.kernel.health.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 12.07.13
 * Time: 23:39
 * To change this template use File | Settings | File Templates.
 */
public class Async {
    protected static final Log LOG = Log.get("async");

    public static <V> Promise<V> promise() {
        return new Promise<V>();
    }

    public static <V> Promise<V> success(V value) {
        Promise<V> result = promise();
        result.success(value);
        return result;
    }

    public static <V> Promise<V> fail(Throwable ex) {
        Promise<V> result = promise();
        result.fail(ex);
        return result;
    }

    public static <V> Promise<List<V>> sequence(List<Promise<V>> list) {
        final Promise<List<V>> result = promise();

        // Create a list with the correct length
        final List<V> resultList = new ArrayList<V>();
        for (int i = 0; i < list.size(); i++) {
            resultList.add(null);
        }

        // Keep track when we're finished
        final CountDownLatch latch = new CountDownLatch(list.size());

        // Iterate over all promises and create a completion handler, which either forwards a failure or which places
        // a successfully computed in the created result list
        int index = 0;
        for (Promise<V> promise : list) {
            final int currentIndex = index;
            promise.onComplete(new CompletionHandler<V>() {
                @Override
                public void onSuccess(V value) throws Exception {
                    if (!result.isFailed()) {
                        // onSuccess can be called from any thread -> sync on resultList...
                        synchronized (resultList) {
                            resultList.set(currentIndex, value);
                        }

                        // Keep track how many results we're waiting for and forward the result when we're finished.
                        latch.countDown();
                        if (latch.getCount() <= 0) {
                            result.success(resultList);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable throwable) throws Exception {
                    result.fail(throwable);
                }
            });
            index++;
        }

        return result;
    }
}
