package sirius.kernel.commons;

import javax.script.ScriptContext;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Context implements Map<String, Object> {

    protected Map<String, Object> data = new TreeMap<String, Object>();
    protected Runnable changeListener;

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return data.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return data.get(key);
    }

    public Value getValue(Object key) {
        return Value.of(get(key));
    }

    @Override
    public Object put(String key, Object value) {
        set(key, value);
        return value;
    }

    public Object putLimited(String key, Object value, int limit) {
        set(key, Strings.limit(value, limit));
        return value;
    }

    @Override
    public Object remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        data.putAll(m);
        if (changeListener != null) {
            changeListener.run();
        }
    }

    @Override
    public void clear() {
        data.clear();
        if (changeListener != null) {
            changeListener.run();
        }
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<Object> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return data.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return data.equals(o);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    /**
     * Creates a new context.
     */
    public static Context create() {
        return new Context();
    }

    public static Context create(Runnable listener) {
        Context result = new Context();
        result.changeListener = listener;
        return result;
    }

    /**
     * Fluent API to fill the context: Sets one value
     */
    public Context set(String key, Object value) {
        Object original = data.put(key, value);
        if (!Strings.areEqual(value, original) && changeListener != null) {
            changeListener.run();
        }
        return this;
    }

    /**
     * Fluent API to fill the context: Sets all given values.
     */
    public Context setAll(Map<String, Object> data) {
        putAll(data);
        if (changeListener != null) {
            changeListener.run();
        }
        return this;
    }

    /**
     * Writes all parameters into the given {@link javax.script.ScriptContext}.
     */
    public void applyTo(ScriptContext ctx) {
        for (Entry<String, Object> e : entrySet()) {
            ctx.setAttribute(e.getKey(), e.getValue(), ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public String toString() {
        return "Context: [" + data + "]";
    }
}
