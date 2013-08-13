/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a map which contains a collection of elements per key.
 * <p>
 * Provides an implementation which simulates a <code>Map&lt;K, Collection&lt;V&gt;&gt;</code> by providing
 * specific <tt>put</tt>, <tt>get</tt> and <tt>remove</tt> methods.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class MultiMap<K, V> {

    /**
     * Creates a new <tt>MultiMap</tt> for the specified types which is not thread safe.
     *
     * @param <K> the type of the keys used in the map
     * @param <V> the type of the values used withing the value lists of the map
     * @return a new instance of <tt>MultiMap</tt> which is not thread safe.
     */
    public static <K, V> MultiMap<K, V> create() {
        return new MultiMap<K, V>(new HashMap<K, Collection<V>>());
    }


    /**
     * Creates a new <tt>MultiMap</tt> for the specified types which is thread safe.
     *
     * @param <K> the type of the keys used in the map
     * @param <V> the type of the values used withing the value lists of the map
     * @return a new instance of <tt>MultiMap</tt> which is thread safe.
     */
    public static <K, V> MultiMap<K, V> createSynchronized() {
        return new MultiMap<K, V>(Collections.synchronizedMap(new HashMap<K, Collection<V>>())) {
            @Override
            public synchronized void put(K key, V value) {
                super.put(key, value);
            }

            @Override
            protected List<V> createValueList() {
                return new CopyOnWriteArrayList<V>();
            }
        };
    }

    protected Map<K, Collection<V>> base;

    /**
     * Used the static factory methods <tt>create</tt> or <tt>createdSynchronized</tt> to obtain an instance.
     */
    protected MultiMap(Map<K, Collection<V>> base) {
        this.base = base;
    }

    /**
     * Adds the given value to the list of values kept for the given key.
     * <p>
     * Note that the values for a given key don't from a <tt>Set</tt>. Therefore adding the same value twice
     * for the same key, will result in having a value list containing the added element twice.
     * </p>
     *
     * @param key   the key for which the value is added to the map
     * @param value the value which is added to the list of values for this key
     */
    public void put(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.get(key);
        if (list == null) {
            list = createValueList();
            base.put(key, list);
        }
        list.add(value);
    }

    /**
     * Sets the given value to the given name.
     * <p>
     * All previously set values will be removed.
     * </p>
     *
     * @param key   the key for which the value is added to the map
     * @param value the name (and only) value for the given key
     */
    public void set(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.get(key);
        if (list == null) {
            list = createValueList();
            base.put(key, list);
        } else {
            list.clear();
        }
        list.add(value);
    }


    /**
     * Can be overridden to specify the subclass of <tt>List</tt> used to store value lists.
     *
     * @return a new instance which is used as value list for a key.
     */
    protected List<V> createValueList() {
        return new ArrayList<V>();
    }

    /**
     * Removes all occurrences of the given value in the value list of the given key.
     * <p>
     * If the value does not occur in the value list or if the key is completely unknown, nothing will happen.
     * </p>
     *
     * @param key   the key of which value list the value will be removed from
     * @param value the value which will be removed from the value list
     */
    public void remove(@Nonnull K key, @Nullable V value) {
        Collection<V> list = base.get(key);
        if (list != null) {
            while (list.remove(value)) {
                //iterate...
            }
        }
    }

    /**
     * Returns the value list for the given key.
     * <p>
     * If the key is completely unknown, an empty list will be returned.
     * </p>
     *
     * @param key the key which value list is to be returned
     * @return the value map associated with the given key or an empty list is the key is unknown
     */
    @Nonnull
    public Collection<V> get(@Nonnull K key) {
        Collection<V> list = base.get(key);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(list);
    }

    /**
     * Returns the set of known keys.
     *
     * @return returns the set of known keys, that is keys for which <tt>put</tt> was called
     */
    @Nonnull
    public Set<K> keySet() {
        return base.keySet();
    }

    /**
     * Provides direct access to the underlying map.
     * <p>
     * For the sake of simplicity and extensibility, the original map is returned. Therefore manipulations should
     * be well considered.
     * </p>
     *
     * @return the underlying <tt>Map</tt> of this instance.
     */
    @Nonnull
    public Map<K, Collection<V>> getUnderlyingMap() {
        return base;
    }

    /**
     * Returns a list of all values for all keys.
     * <p>
     * Note that this list has no <tt>Set</tt> like behaviour. Therefore the same value might occur several times
     * if it was added more than once for the same or for different keys.
     * </p>
     *
     * @return a list of all values stored for all keys
     */
    @Nonnull
    public List<V> values() {
        List<V> result = new ArrayList<V>();
        for (Collection<V> val : getUnderlyingMap().values()) {
            result.addAll(val);
        }
        return result;
    }

    /**
     * Removes all entries from this map
     */
    public void clear() {
        getUnderlyingMap().clear();
    }

    @Override
    public String toString() {
        return base.toString();
    }
}
