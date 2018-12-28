package de.dosmike.sponge.equmatterex.calculator;

import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
public interface MergeFunction<X> {

    X merge (X a, X b);

    default <T,X> Map<T,X> mergeMaps(Map<T,X> map1, Map<T,X> map2) {
        Map<T, X> merged = new HashMap<>(map1);
        for (Map.Entry<T,X> e : map2.entrySet()) {
            merged.put(e.getKey(), map2.getOrDefault(e.getKey(), e.getValue()));
        }
        return merged;
    }

}
