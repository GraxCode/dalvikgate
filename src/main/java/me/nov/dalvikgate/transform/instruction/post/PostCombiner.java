package me.nov.dalvikgate.transform.instruction.post;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.ASMCommons;

public class PostCombiner implements IPostPatcher<MethodNode>, Opcodes {

  @Override
  public void applyPatch(MethodNode mn) {
    InsnList il = mn.instructions;
    for (AbstractInsnNode store : il.toArray()) {
      AbstractInsnNode prev = store.getPrevious();
      if (store.getOpcode() >= -1 && prev != null && isNotRooted(prev) && store.getType() == AbstractInsnNode.VAR_INSN && ASMCommons.isVarStore(store.getOpcode())) {
        VarInsnNode realUsage = findRealUsage((VarInsnNode) store, store.getNext());
        if (realUsage != null) {
          boolean wide = store.getOpcode() == LSTORE || store.getOpcode() == DSTORE;
          il.remove(prev);
          il.remove(store);
          il.insertBefore(realUsage, prev);
          InsnNode dup = new InsnNode(wide ? DUP2 : DUP);
          il.insertBefore(realUsage, dup);
          il.insertBefore(realUsage, store);
          il.remove(realUsage);
          if (!isReused((VarInsnNode) store, store.getNext())) {
            // variable is not reused anymore, we can remove the store instruction
            il.remove(dup);
            il.remove(store);
          }
        }
      }
    }
  }

  private VarInsnNode findRealUsage(VarInsnNode store, AbstractInsnNode ain) {
    if (ain == null || ASMCommons.isReturn(ain) || ASMCommons.isBlockEnd(ain) || ain.getType() == AbstractInsnNode.LABEL) {
      // reached end of block
      return null;
    }
    if (ain.getType() == AbstractInsnNode.VAR_INSN && ain.getOpcode() >= 0) {
      if (ain.getOpcode() == ASMCommons.getOppositeVarOp(store.getOpcode())) {
        if (((VarInsnNode) ain).var == ((VarInsnNode) store).var) {
          // variable is loaded
          return (VarInsnNode) ain;
        }
      }
    }
    return findRealUsage(store, ain.getNext());
  }

  private boolean isReused(VarInsnNode store, AbstractInsnNode ain) {
    if (ASMCommons.isReturn(ain) || ain.getOpcode() == ATHROW) {
      return false;
    }
    if (ain == null || ASMCommons.isBlockEnd(ain) || ain.getType() == AbstractInsnNode.LABEL) {
      // reached end of block, could be reused afterwards
      return true;
    }
    if (ain.getType() == AbstractInsnNode.VAR_INSN && ain.getOpcode() >= 0) {
      if (ain.getOpcode() == ASMCommons.getOppositeVarOp(store.getOpcode())) {
        if (((VarInsnNode) ain).var == ((VarInsnNode) store).var) {
          // variable is reused
          return true;
        }
      }
    }
    return isReused(store, ain.getNext());
  }

  private boolean isNotRooted(AbstractInsnNode prev) {
    switch (prev.getOpcode()) {
    case LDC:
      return true;
    case ACONST_NULL:
      return true;
    case ICONST_M1:
    case ICONST_0:
    case ICONST_1:
    case ICONST_2:
    case ICONST_3:
    case ICONST_4:
    case ICONST_5:
      return true;
    case DCONST_0:
    case DCONST_1:
      return true;
    case LCONST_0:
    case LCONST_1:
      return true;
    case FCONST_0:
    case FCONST_1:
    case FCONST_2:
      return true;
    case NEW:
      return true;
    case BIPUSH:
    case SIPUSH:
      return true;
    case GETSTATIC:
      return true;
    }
    return false;
  }
}
