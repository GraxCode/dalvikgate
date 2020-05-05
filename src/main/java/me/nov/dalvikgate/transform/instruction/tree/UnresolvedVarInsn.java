package me.nov.dalvikgate.transform.instruction.tree;

import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instruction.exception.UnresolvedInsnException;
import me.nov.dalvikgate.transform.instruction.tree.itf.IUnresolvedInstruction;

/**
 * A node that represents a local variable instruction that has not been fully resolved. Due to the ambiguity of android bytecode certain actions are not immediately clear in the
 * same way they would be in plain Java bytecode.
 */
public class UnresolvedVarInsn extends VarInsnNode implements IUnresolvedInstruction, Opcodes {
  private final boolean store;
  private final Type initialType;

  /**
   * Create new unresolved variable instruction.
   *
   * @param store       {@code true} for storage insns.
   * @param initialType Initial type indicated by android instruction. Will be {@code null} if ambiguous.
   */
  public UnresolvedVarInsn(boolean store, Type initialType) {
    super(-1, -1);
    this.store = store;
    this.initialType = initialType;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    validate();
    super.accept(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    validate();
    return super.clone(clonedLabels);
  }

  /**
   * Validate the opcode and variable index are set.
   */
  public void validate() {
    if (DexToASM.noResolve)
      setType(initialType == null ? Type.INT_TYPE : initialType);
    if (opcode < 0)
      throw new UnresolvedInsnException("Variable opcode has not been resolved!");
    if (var < 0)
      throw new UnresolvedInsnException("Variable index has not been resolved!");
  }

  /**
   * Update the instruction's variable index.
   *
   * @param local Variable index.
   */
  public void setLocal(int local) {
    this.var = local;
  }

  public void setType(Type type) {
    switch (type.getSort()) {
    case Type.OBJECT:
    case Type.ARRAY:
      setOpcode(store ? ASTORE : ALOAD);
      break;
    case Type.BOOLEAN:
    case Type.INT:
    case Type.BYTE:
    case Type.SHORT:
    case Type.CHAR:
      setOpcode(store ? ISTORE : ILOAD);
      break;
    case Type.FLOAT:
      setOpcode(store ? FSTORE : FLOAD);
      break;
    case Type.LONG:
      setOpcode(store ? LSTORE : LLOAD);
      break;
    case Type.DOUBLE:
      setOpcode(store ? DSTORE : DLOAD);
      break;
    default:
      throw new IllegalStateException("Unsupported var type: " + type.getDescriptor());
    }
  }
}
