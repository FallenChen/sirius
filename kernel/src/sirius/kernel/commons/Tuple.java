/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import com.google.common.base.Objects;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Represents a tuple of two values with two arbitrary types.
 * <p>
 * If the first type is comparable and should be used to compare the tuples, {@link ComparableTuple} can be used.
 * </p>
 *
 * @param <F> defines the first type of the tuple
 * @param <S> defines the second type of the tuple
 * @author Andreas Haufler (aha@scireum.de)
 * @see Tuple
 * @see Comparable
 * @since 2013/08
 */
public class Tuple<F, S> {

    private F first;
    private S second;

    /**
     * Creates a new tuple with both values set to <tt>null</tt>
     */
    public static <F, S> Tuple<F, S> create() {
        return new Tuple<F, S>(null, null);
    }

    /**
     * Creates a tuple with a given value for <tt>first</tt>
     *
     * @param first defines the value to be used for the first component of the tuple
     */
    public static <F, S> Tuple<F, S> create(F first) {
        return new Tuple<F, S>(first, null);
    }

    /**
     * Creates a tuple with a givens value for <tt>first</tt> and <tt>second</tt>
     *
     * @param first  defines the value to be used for the first component of the tuple
     * @param second defines the value to be used for the second component of the tuple
     */
    public static <F, S> Tuple<F, S> create(F first, S second) {
        return new Tuple<F, S>(first, second);
    }

    /**
     * Creates a tuple with a givens value for <tt>first</tt> and <tt>second</tt>
     * <p>
     * Can be used to specify the generic types for F and S. Otherwise, the <tt>create</tt> methods can be used.
     * </p>
     *
     * @param first  defines the value to be used for the first component of the tuple
     * @param second defines the value to be used for the second component of the tuple
     */
    public Tuple(F first, S second) {
        super();
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the <tt>first</tt> component of the tuple
     *
     * @return the first component of the tuple
     */
    public F getFirst() {
        return first;
    }

    /**
     * Sets the <tt>first</tt> component of the tuple to the given value.
     *
     * @param first defines the value to be used as the first component of the tuple
     */
    public void setFirst(F first) {
        this.first = first;
    }

    /**
     * Returns the <tt>second</tt> component of the tuple
     *
     * @return the second component of the tuple
     */
    public S getSecond() {
        return second;
    }

    /**
     * Sets the <tt>second</tt> component of the tuple to the given value.
     *
     * @param second defines the value to be used as the second component of the tuple
     */
    public void setSecond(S second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Tuple<?, ?>)) {
            return false;
        }
        Tuple<?, ?> other = (Tuple<?, ?>) obj;

        return Objects.equal(first, other.getFirst()) && Objects.equal(second, other.getSecond());
    }

    @Override
    public String toString() {
        return first + ": " + second;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(first, second);
    }

    /**
     * Extracts all <tt>first</tt> components of the given collection of tuples and returns them as list.
     *
     * @param tuples the collection of tuples to process
     * @return a list containing each <tt>first</tt> component of the collection of given tuples.
     */
    public static <T extends Tuple<K, V>, K, V> List<K> firsts(@Nonnull Collection<T> tuples) {
        List<K> result = new ArrayList<K>(tuples.size());
        for (Tuple<K, V> t : tuples) {
            result.add(t.getFirst());
        }
        return result;
    }

    /**
     * Extracts all <tt>second</tt> components of the given collection of tuples and returns them as list.
     *
     * @param tuples the collection of tuples to process
     * @return a list containing each <tt>second</tt> component of the collection of given tuples.
     */
    public static <T extends Tuple<K, V>, K, V> List<V> seconds(@Nonnull Collection<T> tuples) {
        List<V> result = new ArrayList<V>(tuples.size());
        for (Tuple<K, V> t : tuples) {
            result.add(t.getSecond());
        }
        return result;
    }

    /**
     * Converts a map into a list of tuples.
     *
     * @param map the map to be converted
     * @param <K> the key type of the map and therefore the type of the first component of the tuples
     * @param <V> the value type of the map and therefore the type of the second component of the tuples
     * @return a list of tuples, containing one tuple per map entry where the first component is the key,
     *         and the second component is the value of the map entry.
     */
    public static <K, V> List<Tuple<K, V>> fromMap(@Nonnull Map<K, V> map) {
        List<Tuple<K, V>> result = new ArrayList<Tuple<K, V>>(map.size());
        for (Map.Entry<K, V> e : map.entrySet()) {
            result.add(new Tuple<K, V>(e.getKey(), e.getValue()));
        }
        return result;
    }

    /**
     * Converts a collection of tuples into a map
     *
     * @param values the collection of tuples to be converted
     * @param <K>    the key type of the map and therefore the type of the first component of the tuples
     * @param <V>    the value type of the map and therefore the type of the second component of the tuples
     * @return a map containing an entry for each tuple in the collection, where the key is the first component of the
     *         tuple and the value is the second component of the tuple. If two tuples have equal values as first
     *         component, the specific map entry will be overridden in the order defined in the given collection.
     */
    public static <K, V> Map<K, V> toMap(@Nonnull Collection<Tuple<K, V>> values) {
        Map<K, V> result = new HashMap<K, V>();
        for (Tuple<K, V> e : values) {
            result.put(e.getFirst(), e.getSecond());
        }
        return result;
    }

}
