/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.async;

import javax.annotation.Nullable;

/**
 * Maps a given value of type V to a new value of type X.
 * <p>
 * This is intended to be used by {@link Promise#map(Mapper)} or {@link Promise#flatMap(Mapper)}.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public interface Mapper<V, X> {

    /**
     * Maps the given value into a new one.
     *
     * @param value the value to map
     * @return the mapped value for the given input.
     * @throws Exception simplifies error handling, since commonly, errors are forwarded to the next promise.
     */
    @Nullable
    X apply(@Nullable V value) throws Exception;

}
