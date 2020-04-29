package me.nov.dalvikgate.dexlib;

import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.MethodReference;

public class DexLibCommons {
  public static int getSize(MethodParameter p) {
    return p.getType().matches("(J|D)") ? 2 : 1;
  }

  public static String getMethodDesc(MethodReference methodReference) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (CharSequence paramType : methodReference.getParameterTypes()) {
      sb.append(paramType);
    }
    sb.append(')');
    sb.append(methodReference.getReturnType());
    return sb.toString();
  }
}
