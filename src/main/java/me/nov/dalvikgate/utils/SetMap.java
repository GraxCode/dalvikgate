package me.nov.dalvikgate.utils;

import java.util.*;

public class SetMap<K, V> extends TreeMap<K, Set<V>> {
  private static final long serialVersionUID = 1L;

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
