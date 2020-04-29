package me.nov.dalvikgate.transform.instruction.treenode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * TODO
 */
public class UnresolvedVarInsnNode extends VarInsnNode implements Opcodes {

  private int register;
  private boolean store;

  public UnresolvedVarInsnNode(boolean store, int register) {
    super(store ? ASTORE : ALOAD, register/* 0xbadface */); // TODO: remove register
    this.store = store;
    this.register = register;
  }

  @Override
  public int getOpcode() {
    if (store) {
      return ASTORE;
    } else {
      return ALOAD;
    }
  }

  public void updateVar() {

    this.var = 0;
  }

  public int getRegister() {
    return register;
  }

  public boolean isStore() {
    return store;
  }

}
