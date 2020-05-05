package me.nov.dalvikgate.transform.instruction.tree;

import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instruction.exception.UnresolvedInsnException;
import me.nov.dalvikgate.transform.instruction.tree.itf.IUnresolvedInstruction;

public class UnresolvedNumberInsn extends LdcInsnNode implements IUnresolvedInstruction, Opcodes {

  private long wideValue;

  public UnresolvedNumberInsn(long wideValue) {
    super(null);
    this.wideValue = wideValue;
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
   * Validate the opcode is set.
   */
  public void validate() {
    if (DexToASM.noResolve)
      setType(Type.INT_TYPE);
    if (cst == null)
      throw new UnresolvedInsnException("Number const has not been resolved!");
  }

  @Override
  public void setType(Type type) {
    switch (type.getSort()) {
    case Type.BOOLEAN:
    case Type.INT:
    case Type.BYTE:
    case Type.SHORT:
    case Type.CHAR:
      cst = (int) wideValue;
      break;
    case Type.FLOAT:
      cst = Float.intBitsToFloat((int) wideValue);
      break;
    case Type.LONG:
      cst = (long) wideValue;
      break;
    case Type.DOUBLE:
      cst = Double.longBitsToDouble(wideValue);
      break;
    default:
    case Type.OBJECT:
    case Type.ARRAY:
      throw new IllegalStateException("Unsupported var type: " + type.getDescriptor());
    }
  }
}
