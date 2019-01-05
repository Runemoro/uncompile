package uncompile.util;

import java.util.*;

public class Util {
    @SafeVarargs
    public static <T> Set<T> union(Set<T>... sets) {
        Set<T> result = new LinkedHashSet<>();
        for (Set<T> set : sets) {
            if (set != null) {
                result.addAll(set);
            }
        }
        return result;
    }

    public static <K, E> Map<K, Set<E>> deepCopy(Map<K, Set<E>> map) {
        Map<K, Set<E>> copy = new HashMap<>();
        for (Map.Entry<K, Set<E>> entry : map.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}
