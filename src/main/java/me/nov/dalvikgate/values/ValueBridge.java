package me.nov.dalvikgate.values;

import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedStringEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableNullEncodedValue;

public class ValueBridge {

  public static String arrayToString(DexBackedArrayEncodedValue sig) {
    StringBuilder sb = new StringBuilder();
    for (EncodedValue ev : sig.getValue()) {
      sb.append(((DexBackedStringEncodedValue) ev).getValue());
    }
    return sb.toString();
  }

  public static Object toObject(EncodedValue ev) {
    if (ev == null || ev instanceof ImmutableNullEncodedValue)
      return null;
    if (ev instanceof DexBackedStringEncodedValue) {
      return ((DexBackedStringEncodedValue) ev).getValue();
    } else if (ev.getClass().getName().startsWith("org.jf.dexlib2.immutable.value.Immutable")) {
      try {
        return ev.getClass().getMethod("getValue").invoke(ev);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException(ev.getClass().getName());
  }

}
