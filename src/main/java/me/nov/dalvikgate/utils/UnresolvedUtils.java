package me.nov.dalvikgate.utils;

import java.util.List;

import org.jf.dexlib2.Opcode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;

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

  /**
   * Check if the instruction immediately given in the contributing insns list is this given insn... unless its a variable load. Then check the next insn.
   *
   * @param insn  Given instruction
   * @param value Value to check.
   * @return {@code true} if the given instruction is responsible for the given value.
   */
  public static boolean isDirectlyResponsible(AbstractInsnNode insn, AbstractValue value) {
    List<AbstractInsnNode> insns = value.getInsns();
    AbstractInsnNode top = null;
    int offset = 1;
    while (offset <= insns.size() && (top = insns.get(insns.size() - offset)) instanceof VarInsnNode && !top.equals(insn))
      offset++;
    return insn.equals(top);
  }

  public static boolean containsUnresolved(List<AbstractInsnNode> insns) {
    return insns.stream().anyMatch(insn -> insn instanceof IUnresolvedInstruction && !((IUnresolvedInstruction) insn).isResolved());
  }
}
