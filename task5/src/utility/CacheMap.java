package utility;

import java.util.TreeMap;

public class CacheMap<K extends Comparable<K>,V>
{
    private final int capacity;
    private TreeMap<K,V> map  = new TreeMap<>();

    public CacheMap(int capacity) { this.capacity = capacity; }

    public V get(K key) { return map.get(key); }

    public void put(K key, V value) {
        if(map.size() >= capacity) {
            map.remove(map.firstKey());
        }
        map.put(key, value);
    }
}
