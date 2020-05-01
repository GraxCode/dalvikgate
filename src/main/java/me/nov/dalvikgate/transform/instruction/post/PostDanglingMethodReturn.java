package me.nov.dalvikgate.transform.instruction.post;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;

/**
 * Transformer that adds {@code POP} instructions after every stack-pushing method without a bound variable store. This has to be run <i>BEFORE</i> other transformes that change
 * variables.
 */
public class PostDanglingMethodReturn implements ITransformer<MethodNode, MethodNode>, Opcodes {

  @Override
  public void build(MethodNode mn) {
    InsnList il = mn.instructions;
    for (AbstractInsnNode ain : il.toArray()) {
      AbstractInsnNode prev = ain.getPrevious();
      if (prev != null && prev.getType() == AbstractInsnNode.METHOD_INSN) {
        MethodInsnNode min = (MethodInsnNode) prev;
        if (ain.getType() != AbstractInsnNode.VAR_INSN || (ain.getOpcode() != -1 && !ASMCommons.isVarStore(ain.getOpcode()))) {
          int descSize = Type.getReturnType(min.desc).getSize();
          if (descSize > 0) {
            il.insertBefore(ain, new InsnNode(descSize > 1 ? POP2 : POP));
          }
        }
      }
    }
    mn.maxStack = Math.max(mn.maxStack, 2);
  }

  @Override
  public MethodNode getTransformed() {
    throw new IllegalArgumentException();
  }
}
