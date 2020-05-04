package me.nov.dalvikgate.transform.instruction.post;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.ASMCommons;

/**
 * Transformer that converts successive local store and load instructions with the same local var to {@code dup}; {@code store}.
 */
public class PostDupInserter implements IPostPatcher<MethodNode>, Opcodes {

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
            boolean wide = prev.getOpcode() == LSTORE || prev.getOpcode() == DSTORE;
            il.set(store, new InsnNode(wide ? DUP2 : DUP));
            il.set(load, new VarInsnNode(store.getOpcode(), load.var));
          }
        }
      }
    }
    mn.maxStack = Math.max(mn.maxStack, 2);
  }
}
