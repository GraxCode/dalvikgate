package me.nov.dalvikgate.utils;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SetMap<K, V> extends TreeMap<K, Set<V>> {
  public void putSingle(K key, V value) {
    computeIfAbsent(key, k -> new TreeSet<>()).add(value);
  }

  public boolean contains(K key, V value) {
    Set<V> set = get(key);
    if (set == null)
      return false;
    return set.contains(value);
  }
}
