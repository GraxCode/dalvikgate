package me.nov.dalvikgate.utils;

import org.jf.dexlib2.Opcode;
import org.objectweb.asm.Opcodes;

public class UnresolvedUtils implements Opcodes {
  public static int getDefaultVarOp(boolean store) {
    return store ? ASTORE : ALOAD;
  }

  public static int getDefaultWideArrayOp(boolean store) {
    return store ? DASTORE : DALOAD;
  }

  public static int getDefaultJumpOp(Opcode dalvikOp) {
    switch (dalvikOp) {
    case IF_EQ:
      return IF_ACMPEQ;
    case IF_NE:
      return IF_ACMPNE;
    case IF_EQZ:
      return IFNULL;
    case IF_NEZ:
      return IFNONNULL;
    default:
      throw new IllegalStateException("Jump cannot be unresolved! Got " + dalvikOp.name);
    }
  }
}
