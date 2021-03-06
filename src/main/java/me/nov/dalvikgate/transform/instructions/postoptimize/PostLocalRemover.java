package me.nov.dalvikgate.transform.instructions.postoptimize;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.ASMCommons;

/**
 * Transformer that removes successive local store and load instructions with locals that are unused later.
 */
public class PostLocalRemover implements IPostPatcher<MethodNode>, Opcodes {

  @Override
  public void applyPatch(MethodNode mn) {
    InsnList il = mn.instructions;
    for (AbstractInsnNode ain : il.toArray()) {
      AbstractInsnNode prev = ain.getPrevious(); // no label skip as it could be a jump target
      if (prev != null && ain.getType() == AbstractInsnNode.VAR_INSN && prev.getType() == AbstractInsnNode.VAR_INSN) {
        VarInsnNode store = (VarInsnNode) prev;
        VarInsnNode load = (VarInsnNode) ain;
        if (store.var != -1 && store.var == load.var && store.getOpcode() >= 0 && load.getOpcode() >= 0) {
          if (ASMCommons.isVarStore(store.getOpcode()) && ASMCommons.getOppositeVarOp(store.getOpcode()) == load.getOpcode()) {
            if (canRemove(prev, ain.getNext())) {
              il.remove(store);
              il.remove(load);
            }
          }
        }
      }
    }
    mn.maxStack = Math.max(mn.maxStack, 2);
  }

  private boolean canRemove(AbstractInsnNode store, AbstractInsnNode ain) {
    if (ain == null || ASMCommons.isReturn(ain)) {
      // reached end of code, nothing is executed anymore
      return true;
    }
    if (ASMCommons.isBlockEnd(ain)) {
      // reached end of subroutine, code continues somewhere else and variable can still be loaded
      return false;
    }
    if (ain.getType() == AbstractInsnNode.VAR_INSN && ain.getOpcode() >= 0) {
      if (ain.getOpcode() == store.getOpcode()) {
        if (((VarInsnNode) ain).var == ((VarInsnNode) store).var) {
          // variable is stored somewhere else and not loaded before
          return true;
        }
      }
      if (ain.getOpcode() == ASMCommons.getOppositeVarOp(store.getOpcode())) {
        if (((VarInsnNode) ain).var == ((VarInsnNode) store).var) {
          // variable is reused
          return false;
        }
      }
    }
    return canRemove(store, ain.getNext());
  }
}
