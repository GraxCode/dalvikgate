package me.nov.dalvikgate.dexlib;

import org.jf.dexlib2.iface.MethodParameter;

public class DexLibCommons {
  public static int getSize(MethodParameter p) {
    return p.getType().matches("(J|D)") ? 2 : 1;
  }
}
