package me.nov.dalvikgate.asm;

import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class ASMCommons implements Opcodes {
  public static final Type OBJECT_TYPE = Type.getType("Ljava/lang/Object;");
  public static final Type ARRAY_TYPE = Type.getType("[Ljava/lang/Object;");

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

  public static AbstractInsnNode makeLongPush(long l) {
    if (l == 0) {
      return new InsnNode(LCONST_0);
    } else if (l == 1) {
      return new InsnNode(LCONST_1);
    }
    return new LdcInsnNode(l);
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
    text = "dalvikgate: " + text;
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

  public static Type getTypeForDesc(CharSequence cs) {
    String desc = (String) cs;
    switch (desc.charAt(0)) {
    case 'L':
      return OBJECT_TYPE;
    case '[':
      return ARRAY_TYPE;
    case 'I':
    case 'S':
    case 'Z':
    case 'C':
    case 'B':
      return Type.INT_TYPE;
    case 'J':
      return Type.LONG_TYPE;
    case 'D':
      return Type.DOUBLE_TYPE;
    case 'F':
      return Type.FLOAT_TYPE;
    }
    throw new IllegalArgumentException("illegal desc: " + cs);
  }

  public static int getOppositeVarOp(int op) {
    switch (op) {
    case ASTORE:
      return ALOAD;
    case ISTORE:
      return ILOAD;
    case LSTORE:
      return LLOAD;
    case DSTORE:
      return DLOAD;
    case FSTORE:
      return FLOAD;
    case ALOAD:
      return ASTORE;
    case ILOAD:
      return ISTORE;
    case LLOAD:
      return LSTORE;
    case DLOAD:
      return DSTORE;
    case FLOAD:
      return FSTORE;
    }
    throw new IllegalArgumentException("illegal op: " + op);
  }

  public static boolean isVarStore(int op) {
    switch (op) {
    case ASTORE:
      return true;
    case ISTORE:
      return true;
    case LSTORE:
      return true;
    case DSTORE:
      return true;
    case FSTORE:
      return true;
    case ALOAD:
      return false;
    case ILOAD:
      return false;
    case LLOAD:
      return false;
    case DLOAD:
      return false;
    case FLOAD:
      return false;
    }
    throw new IllegalArgumentException("illegal op: " + op);
  }

  public static int getPrimitiveIndex(String primitive) {
    if (primitive.length() > 1) {
      throw new IllegalArgumentException("not a primitive: " + primitive);
    }
    switch (primitive.charAt(0)) {
    case 'Z':
      return 4;
    case 'C':
      return 5;
    case 'F':
      return 6;
    case 'D':
      return 7;
    case 'B':
      return 8;
    case 'S':
      return 9;
    case 'I':
      return 10;
    case 'J':
      return 11;
    }
    throw new IllegalArgumentException("not a primitive: " + primitive);
  }

  public static Type getPushedTypeForInsn(AbstractInsnNode insn) {
    int op = insn.getOpcode();
    switch (insn.getType()) {
    case AbstractInsnNode.FIELD_INSN:
      return Type.getType(((FieldInsnNode) insn).desc);
    case AbstractInsnNode.METHOD_INSN:
      return Type.getType(((MethodInsnNode) insn).desc).getReturnType();
    case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
      return Type.getType(((InvokeDynamicInsnNode) insn).desc).getReturnType();
    case AbstractInsnNode.INT_INSN:
      if (op == NEWARRAY)
        return ARRAY_TYPE;
      return Type.INT_TYPE;
    case AbstractInsnNode.MULTIANEWARRAY_INSN:
      return ARRAY_TYPE;
    case AbstractInsnNode.INSN:
      // TODO: Validate that we do not write "ICONST_0" in cases where we meant to make a "null" constant
      // TODO: Validate that we do not write "ACONST_NULL" in cases where we meant to make a "0" constant
      if (op == ICONST_M1 || (op >= ICONST_0 && op <= ICONST_5))
        return Type.INT_TYPE;
      else if (op == LCONST_0 || op == LCONST_1)
        return Type.LONG_TYPE;
      else if (op == FCONST_0 || op == FCONST_1 || op == FCONST_2)
        return Type.FLOAT_TYPE;
      else if (op == DCONST_0 || op == DCONST_1)
        return Type.DOUBLE_TYPE;
      else if (op == ACONST_NULL)
        return OBJECT_TYPE;
      break;
    case AbstractInsnNode.TYPE_INSN:
      String typeInternal = ((TypeInsnNode) insn).desc;
      if (typeInternal.startsWith("[") || op == ANEWARRAY)
        return ARRAY_TYPE;
      else
        return OBJECT_TYPE;
    case AbstractInsnNode.LDC_INSN:
      Object cst = ((LdcInsnNode) insn).cst;
      if (cst instanceof Long)
        return Type.LONG_TYPE;
      else if (cst instanceof Double)
        return Type.DOUBLE_TYPE;
      else if (cst instanceof Float)
        return Type.FLOAT_TYPE;
      else if (cst instanceof Integer)
        return Type.INT_TYPE;
      else
        return OBJECT_TYPE;
    case AbstractInsnNode.VAR_INSN:
      if (op == ASTORE || op == ALOAD)
        return OBJECT_TYPE;
      else if (op == ISTORE || op == ILOAD)
        return Type.INT_TYPE;
      else if (op == LSTORE || op == LLOAD)
        return Type.LONG_TYPE;
      else if (op == FSTORE || op == FLOAD)
        return Type.FLOAT_TYPE;
      else if (op == DSTORE || op == DLOAD)
        return Type.DOUBLE_TYPE;
    }
    throw new IllegalArgumentException(insn.getClass().getSimpleName() + " - OP: " + insn.getOpcode());
  }

  public static boolean isBlockEnd(AbstractInsnNode ain) {
    return isReturn(ain) || ain.getType() == AbstractInsnNode.JUMP_INSN || ain.getOpcode() == ATHROW;
  }

  public static boolean isReturn(AbstractInsnNode ain) {
    switch (ain.getOpcode()) {
    case RETURN:
    case ARETURN:
    case DRETURN:
    case FRETURN:
    case IRETURN:
    case LRETURN:
      return true;
    default:
      return false;
    }
  }

  public static AbstractInsnNode getRealNext(AbstractInsnNode ain) {
    do {
      // skip labels, frames and line numbers
      ain = ain.getNext();
    } while (ain != null && isIdle(ain));
    return ain;
  }

  public static AbstractInsnNode getRealPrevious(AbstractInsnNode ain) {
    do {
      // skip labels, frames and line numbers
      ain = ain.getPrevious();
    } while (ain != null && isIdle(ain));
    return ain;
  }

  public static AbstractInsnNode getRealLast(InsnList il) {
    AbstractInsnNode ain = il.getLast();
    while (ain != null && isIdle(ain)) {
      // skip labels, frames and line numbers
      ain = ain.getPrevious();
    }
    return ain;
  }

  public static boolean isIdle(AbstractInsnNode ain) {
    return ain.getType() == AbstractInsnNode.FRAME || ain.getType() == AbstractInsnNode.LABEL || ain.getType() == AbstractInsnNode.LINE || ain.getOpcode() == NOP;
  }
}
