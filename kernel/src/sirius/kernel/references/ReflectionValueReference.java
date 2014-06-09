/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.kernel.references;

import sirius.kernel.commons.Reflection;
import sirius.kernel.commons.Strings;
import sirius.kernel.nls.NLS;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a {@link ValueReference} based on reflection.
 * <p>
 * This will look utilize setXXX, getXXX or isXXX methods to read or write a given property.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class ReflectionValueReference<T, V> implements ValueReference<T, V> {

    private Method[] getter;
    private Method setter;
    private final String path;

    /**
     * Creates a new reference based on the given methods and representing the given path.
     * <p>
     * If the path contains multiple hops (dot-separated). First all except the last getters are invoked and then
     * either the setter or the last getter are invoked. Otherwise just the getter or setter is invoked.
     * </p>
     *
     * @param path   the access path represented by this value reference
     * @param setter the setter method used to the the value
     * @param getter the path of getters used to the the final object and the last getter to finally access the value
     */
    public ReflectionValueReference(String path, Method setter, Method... getter) {
        this.path = path;
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getValue(T item) {
        try {
            Object result = item;
            for (Method g : getter) {
                if (result == null) {
                    break;
                }
                result = g.invoke(result, new Object[0]);
            }
            return (V) result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the value converted to a user readable string.
     *
     * @param item the object to read the value from
     * @return a string representation of the value read from the given item
     */
    @Nonnull
    public String getValueString(@Nullable T item) {
        return NLS.toUserString(getValue(item));
    }

    @Override
    public void setValue(T data, V value) {
        try {
            Object result = data;
            for (int i = 0; i < getter.length - 1; i++) {
                if (result == null) {
                    break;
                }
                result = getter[i].invoke(result, new Object[0]);
            }
            if (result != null) {
                setter.invoke(result, value);
            }
        } catch (Exception e) {
            // IGNORED
        }
    }

    /**
     * Creates a new <tt>ReflectionValueReference</tt> for the given class and property.
     *
     * @param clazz the class which provides the getter/setter methods
     * @param field the name of the field or property which should be represented by this reference
     * @return a new value reference providing access to the given field in the given class.
     */
    public static <T, V> ReflectionValueReference<T, V> create(Class<T> clazz, String field) {
        String[] path = field.split("\\.");
        List<Method> result = new ArrayList<Method>(path.length);
        Method setter = null;
        Class<?> c = clazz;
        for (int i = 0; i < path.length; i++) {
            Method m = Reflection.getter(c, path[i]);
            result.add(m);
            if (i == path.length - 1) {
                try {
                    setter = Reflection.setter(c, path[i], m.getReturnType());
                } catch (Exception e) {
                    // IGNORE
                }
            }
            c = m.getReturnType();
        }
        return new ReflectionValueReference<T, V>(field, setter, result.toArray(new Method[result.size()]));
    }

    /**
     * Returns the type which is read/written by this value reference
     *
     * @return the type read and written by this reference.
     */
    public Class<?> getType() {
        return getter[0].getReturnType();
    }

    /**
     * Determins if the value is writable (if a setter was found).
     *
     * @return <tt>true</tt> if a setter was found, <tt>false</tt> otherwise
     */
    public boolean isWritable() {
        return setter != null;
    }

    @Override
    public String toString() {
        return path;
    }

    /**
     * Returns the access path represented by this reference
     *
     * @return a dot-separated path which is represented by this reference
     */
    public String getPath() {
        return path;
    }
}
