package uncompile.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FakeMap<K, V> implements Map<K, V> {
    private Class<K> keyClass;
    private Function<K, V> function;

    public FakeMap(Class<K> keyClass, Function<K, V> function) {
        this.keyClass = keyClass;
        this.function = function;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return keyClass.isAssignableFrom(key.getClass());
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return keyClass.isAssignableFrom(key.getClass()) ? function.apply((K) key) : null;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
