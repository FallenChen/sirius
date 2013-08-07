package sirius.kernel.references;

import com.scireum.common.Tools;
import com.scireum.common.nls.NLS;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a {@link ValueReference} based on reflection.
 */
public class ReflectionValueReference<T, V> implements ValueReference<T, V> {

    private Method[] getter;
    private Method setter;
    private final String path;

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

    public String getValueString(T item) {
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

    public static <T, V> ReflectionValueReference<T, V> create(Class<T> clazz, String field) {
        String[] path = field.split("\\.");
        List<Method> result = new ArrayList<Method>(path.length);
        Method setter = null;
        Class<?> c = clazz;
        for (int i = 0; i < path.length; i++) {
            Method m = Tools.getter(c, path[i]);
            result.add(m);
            if (i == path.length - 1) {
                try {
                    setter = Tools.setter(c, path[i], m.getReturnType());
                } catch (Exception e) {
                    // IGNORE
                }
            }
            c = m.getReturnType();
        }
        return new ReflectionValueReference<T, V>(field, setter, result.toArray(new Method[result.size()]));
    }

    public Class<?> getType() {
        return getter[0].getReturnType();
    }

    public boolean isWritable() {
        return setter != null;
    }

    @Override
    public String toString() {
        return path;
    }

    public String getPath() {
        return path;
    }
}
