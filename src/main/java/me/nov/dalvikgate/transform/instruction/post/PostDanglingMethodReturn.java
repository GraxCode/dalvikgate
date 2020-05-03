package me.nov.dalvikgate.transform.instruction.post;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;

/**
 * Transformer that adds {@code POP} instructions after every stack-pushing method without a bound variable store. This has to be run <i>BEFORE</i> other transformers that change
 * variables.
 */
public class PostDanglingMethodReturn implements ITransformer<MethodNode, MethodNode>, Opcodes {

  @Override
  public void build(MethodNode mn) {
    InsnList il = mn.instructions;
    for (AbstractInsnNode ain : il.toArray()) {
      if (ASMCommons.isIdle(ain))
        continue;
      AbstractInsnNode prev = ASMCommons.getRealPrevious(ain);
      if (prev != null && prev.getType() == AbstractInsnNode.METHOD_INSN) {
        MethodInsnNode min = (MethodInsnNode) prev;
        if (ain.getType() != AbstractInsnNode.VAR_INSN || (ain.getOpcode() != -1 && !ASMCommons.isVarStore(ain.getOpcode()))) {
          int descSize = Type.getReturnType(min.desc).getSize();
          if (descSize > 0) {
            il.insert(prev, new InsnNode(descSize > 1 ? POP2 : POP));
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
