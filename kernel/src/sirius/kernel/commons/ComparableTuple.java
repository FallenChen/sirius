/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

/**
 * Provides a tuple of values where the key is used as comparator.
 * <p>
 * Subclasses {@link Tuple} to implement <tt>Comparable</tt> based on the key, that is the first element of the tuple.
 * </p>
 *
 * @param <F> defines the first type of the tuple. The supplied class must implement <code>Comparable</code>
 * @param <S> defines the second type of the tuple
 * @author Andreas Haufler (aha@scireum.de)
 * @see Tuple
 * @see Comparable
 * @since 1.0
 */
public class ComparableTuple<F extends Comparable<F>, S> extends Tuple<F, S> implements Comparable<ComparableTuple<F, S>> {

    /**
     * Creates a new tuple without any values.
     */
    public ComparableTuple() {
        super();
    }

    /**
     * Creates a new tuple with the given values.
     *
     * @param first defines the first value of the tuple
     * @param second defines the second value of the tuple
     */
    public ComparableTuple(F first, S second) {
        super(first, second);
    }

    /**
     * Creates a new tuple by only specifying the first value of the tuple.
     * <p>
     *     The second value will remain <tt>null</tt>.
     * </p>
     * @param first defines the first value of the tuple
     */
    public ComparableTuple(F first) {
        super(first);
    }

    @Override
    public int compareTo(ComparableTuple<F, S> o) {
        if (o == null) {
            return 1;
        }
        if (o.getFirst() == null && getFirst() != null) {
            return 1;
        } else if (getFirst() == null) {
            return 0;
        }
        if (getFirst() == null) {
            return -1;
        }
        return getFirst().compareTo(o.getFirst());
    }

}
