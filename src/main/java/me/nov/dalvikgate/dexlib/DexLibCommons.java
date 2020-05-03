package me.nov.dalvikgate.dexlib;

import static org.objectweb.asm.Opcodes.*;

import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.base.value.BaseStringEncodedValue;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.dexbacked.value.*;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.*;
import org.jf.dexlib2.immutable.value.ImmutableNullEncodedValue;
import org.objectweb.asm.*;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.instruction.exception.TranslationException;

public class DexLibCommons {
  public static int getSize(MethodParameter p) {
    return p.getType().matches("(J|D)") ? 2 : 1;
  }

  public static String getMethodDesc(MethodReference methodReference) {
    return ASMCommons.buildMethodDesc(methodReference.getParameterTypes(), methodReference.getReturnType());
  }

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
    if (ev instanceof BaseStringEncodedValue) {
      return ((BaseStringEncodedValue) ev).getValue();
    } else if (ev.getClass().getName().startsWith("org.jf.dexlib2.immutable.value.Immutable")) {
      try {
        return ev.getClass().getMethod("getValue").invoke(ev);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else if (ev instanceof MethodHandleEncodedValue) {
      return referenceToASMHandle(((MethodHandleEncodedValue) ev).getValue());
    } else if (ev instanceof MethodTypeEncodedValue) {
      MethodProtoReference proto = ((MethodTypeEncodedValue) ev).getValue();
      return Type.getMethodType(ASMCommons.buildMethodDesc(proto.getParameterTypes(), proto.getReturnType()));
    }
    throw new IllegalArgumentException(ev.getClass().getName());
  }

  /**
   * Generates a possible desc for a quick invoke
   */
  public static String generateFakeQuickDesc(int regCount, BuilderInstruction next) {
    StringBuilder sb = new StringBuilder("(");
    // skip object reference
    for (int j = 1; j < regCount; j++) {
      sb.append("Ljava/lang/Object;"); // no way to tell if there is a long in the desc, could produce invalid code
    }
    sb.append(")");
    switch (next.getOpcode()) {
    case MOVE_RESULT:
      sb.append("I");
      break;
    case MOVE_RESULT_OBJECT:
      sb.append("Ljava/lang/Object;");
      break;
    case MOVE_RESULT_WIDE:
      sb.append("J"); // could be D too
      break;
    default:
      sb.append("V");
      break;
    }
    return sb.toString();
  }

  public static Handle referenceToASMHandle(MethodHandleReference handleRef) {
    if (handleRef.getMemberReference() instanceof MethodReference) {
      MethodReference method = (MethodReference) handleRef.getMemberReference();
      int tag = convertHandleTag(handleRef.getMethodHandleType());
      return new Handle(tag, Type.getType(method.getDefiningClass()).getInternalName(), method.getName(), ASMCommons.buildMethodDesc(method.getParameterTypes(), method.getReturnType()),
          tag == H_INVOKEINTERFACE);
    }
    throw new TranslationException("unsupported handle type " + handleRef.getClass());
  }

  public static int convertHandleTag(int methodHandleType) {
    switch (methodHandleType) {
    case MethodHandleType.STATIC_PUT:
      return H_PUTSTATIC;
    case MethodHandleType.STATIC_GET:
      return H_GETSTATIC;
    case MethodHandleType.INSTANCE_PUT:
      return H_PUTFIELD;
    case MethodHandleType.INSTANCE_GET:
      return H_GETFIELD;
    case MethodHandleType.INVOKE_STATIC:
      return H_INVOKESTATIC;
    case MethodHandleType.INVOKE_DIRECT:
    case MethodHandleType.INVOKE_INSTANCE:
      return H_INVOKEVIRTUAL;
    case MethodHandleType.INVOKE_CONSTRUCTOR:
      return H_INVOKESPECIAL;
    case MethodHandleType.INVOKE_INTERFACE:
      return H_INVOKEINTERFACE;
    }
    throw new TranslationException("unsupported handle tag " + methodHandleType);
  }
}
