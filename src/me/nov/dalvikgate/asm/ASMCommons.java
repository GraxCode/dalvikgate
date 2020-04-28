package me.nov.dalvikgate.asm;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class ASMCommons implements Opcodes {

  public static AbstractInsnNode makeNullPush(Type type) {
    switch (type.getSort()) {
    case Type.OBJECT:
    case Type.ARRAY:
      return new InsnNode(ACONST_NULL);
    case Type.VOID:
      return new InsnNode(NOP);
    case Type.DOUBLE:
      return new InsnNode(DCONST_0);
    case Type.FLOAT:
      return new InsnNode(FCONST_0);
    case Type.LONG:
      return new InsnNode(LCONST_0);
    default:
      return new InsnNode(ICONST_0);
    }
  }

  public static AbstractInsnNode makeIntPush(int i) {
    if (i >= -1 && i <= 5) {
      return new InsnNode(i + 3); // iconst_i
    }
    if (i >= -128 && i <= 127) {
      return new IntInsnNode(BIPUSH, i);
    }

    if (i >= -32768 && i <= 32767) {
      return new IntInsnNode(SIPUSH, i);
    }
    return new LdcInsnNode(i);
  }

  public static InsnList makeExceptionThrow(String name, String text) {
    InsnList il = new InsnList();
    il.add(new TypeInsnNode(NEW, name));
    il.add(new InsnNode(DUP));
    il.add(new LdcInsnNode(text));
    il.add(new MethodInsnNode(INVOKESPECIAL, name, "<init>", "(Ljava/lang/String;)V"));
    il.add(new InsnNode(ATHROW));
    return il;
  }

  public static String buildMethodDesc(List<? extends CharSequence> parameterTypes, String returnType) {
    return Type.getMethodDescriptor(Type.getType(returnType), parameterTypes.stream().map(s -> Type.getType((String) s)).toArray(Type[]::new));
  }

  public static int getLoadOpForDesc(CharSequence cs) {
    String desc = (String) cs;
    switch (desc.charAt(0)) {
    case 'L':
    case '[':
      return ALOAD;
    case 'I':
    case 'S':
    case 'Z':
    case 'C':
    case 'B':
      return ILOAD;
    case 'J':
      return LLOAD;
    case 'D':
      return DLOAD;
    case 'F':
      return FLOAD;
    }
    throw new IllegalArgumentException("illegal desc: " + cs);
  }

  public static int getStoreOpForDesc(CharSequence cs) {
    String desc = (String) cs;
    switch (desc.charAt(0)) {
    case 'L':
    case '[':
      return ASTORE;
    case 'I':
    case 'S':
    case 'Z':
    case 'C':
    case 'B':
      return ISTORE;
    case 'J':
      return LSTORE;
    case 'D':
      return DSTORE;
    case 'F':
      return FSTORE;
    }
    throw new IllegalArgumentException("illegal desc: " + cs);
  }
}
