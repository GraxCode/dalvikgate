package me.nov.dalvikgate.dexlib;

import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.MethodReference;

import me.nov.dalvikgate.asm.ASMCommons;

public class DexLibCommons {
  public static int getSize(MethodParameter p) {
    return p.getType().matches("(J|D)") ? 2 : 1;
  }

  public static String getMethodDesc(MethodReference methodReference) {
    return ASMCommons.buildMethodDesc(methodReference.getParameterTypes(), methodReference.getReturnType());
  }
}
