package me.nov.dalvikgate.transform.instruction.post;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;

public class PostDupInserter implements ITransformer<MethodNode, MethodNode>, Opcodes {

  @Override
  public void build(MethodNode mn) {
    InsnList il = mn.instructions;
    for (AbstractInsnNode ain : il.toArray()) {
      AbstractInsnNode prev = ain.getPrevious();
      if (prev != null && ain.getType() == AbstractInsnNode.VAR_INSN && prev.getType() == AbstractInsnNode.VAR_INSN) {
        VarInsnNode store = (VarInsnNode) prev;
        VarInsnNode load = (VarInsnNode) ain;
        if (store.var != -1 && store.var == load.var && store.getOpcode() >= 0 && load.getOpcode() >= 0) {
          if (ASMCommons.isVarStore(prev.getOpcode()) && !ASMCommons.isVarStore(ain.getOpcode())) {
            boolean wide = prev.getOpcode() == LSTORE || prev.getOpcode() == DSTORE;
            il.set(store, new InsnNode(wide ? DUP2 : DUP));
            il.set(load, new VarInsnNode(ASMCommons.getOppositeVarOp(load.getOpcode()), load.var));
          }
        }
      }
    }
    mn.maxStack = Math.max(mn.maxStack, 1);
  }

  @Override
  public MethodNode getTransformed() {
    throw new IllegalArgumentException();
  }
}
