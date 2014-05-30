/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * Helper class which provides various methods to work with lambdas.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/05
 */
public class Lambdas {

    /**
     * Provides an identity function which permits to "touch" the element for which it was called.
     * <p>
     * This is designed to be used with {@link java.util.Optional} like this:
     * <code>return Optional.of("Test").map(Lambdas.touch(s -> System.out.println(s)))</code>
     * </p>
     *
     * @param consumer the lambda used to "touch" (use) each parameter
     * @return an identity function which also applies the <tt>consumer</tt> on each parameter
     */
    public static <T> Function<T, T> touch(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return t;
        };
    }

    /**
     * Used to collect the results of a stream operation into an existing collection.
     *
     * @param collection the target collection
     * @return a {@link java.util.stream.Collector} inserting all elements into the given collection
     */
    public static <T, C extends Collection<T>> Collector<T, ?, C> into(C collection) {
        return Collector.of(() -> collection, Collection::add, (a, b) -> {
            throw new UnsupportedOperationException();
        }, Function.identity(), Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Filters the given collection using the given predicate.
     * <p>
     * In contrast to the stream API of Java, this MODIFIES the collection which is passed in. This is certainly
     * no the "functional way" to do it - but Java not being a functional language there are some use cases to
     * modify an existing collection instead of creating a new one.
     * </p>
     *
     * @param collection to collection to remove elements from
     * @param filter     the filter which evaluates to <tt>true</tt> for each element to be removed
     * @return the filtered (modified) collection
     */
    public static <T, C extends Collection<T>> C remove(C collection, Predicate<T> filter) {
        Iterator<T> iter = collection.iterator();
        while (iter.hasNext()) {
            if (filter.test(iter.next())) {
                iter.remove();
            }
        }
        return collection;
    }

}
