package me.nov.dalvikgate.transform.instructions.unresolved;

import java.util.Map;

import me.coley.analysis.util.FrameUtil;
import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.utils.UnresolvedUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;
import org.objectweb.asm.tree.analysis.Frame;

public class UnresolvedWideArrayInsn extends InsnNode implements IUnresolvedInstruction, Opcodes {
  private boolean store;
  private boolean resolvedType;
  private boolean resolvedOp;

  /**
   * Create new unresolved wide array store or load instruction.
   *
   * @param store {@code true} for storage insns.
   */
  public UnresolvedWideArrayInsn(boolean store) {
    super(UnresolvedUtils.getDefaultWideArrayOp(store));
    this.store = store;
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
      setType(Type.LONG_TYPE);
    if (opcode < 0)
      throw new UnresolvedInsnException("Array store / load opcode has not been resolved!");
  }

  /**
   * @param type either DOUBLE or LONG
   */
  public void setType(Type type) {
    switch (type.getSort()) {
    case Type.DOUBLE:
      setOpcode(store ? DASTORE : DALOAD);
      break;
    case Type.LONG:
      setOpcode(store ? LASTORE : LALOAD);
      break;
    default:
      throw new IllegalArgumentException("Only wide arrays allowed! Got " + type.getDescriptor());
    }
    resolvedType = true;
  }

  public void setOpcode(int opcode) {
    this.opcode = opcode;
    resolvedOp = true;
  }

  @Override
  public boolean isResolved() {
    return resolvedType && resolvedOp;
  }

  @Override
  public boolean tryResolve(int index, MethodNode method, Frame<AbstractValue>[] frames) {
    // STORE: arrayref, index, value -> _
    // LOAD:: arrayref, index -> value
    if (store) {
      // Check value on stack being stored
      AbstractValue value = FrameUtil.getTopStack(frames[index]);
      setType(value.getType());
    } else {
      // arrayref, index -> value
      AbstractValue arrayRef = FrameUtil.getStackFromTop(frames[index], 1);
      setType(arrayRef.getType().getElementType());
    }
    return isResolved();
  }
}
