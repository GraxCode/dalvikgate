package me.nov.dalvikgate.transform.instructions.unresolved;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;

public class UnresolvedNumberInsn extends LdcInsnNode implements IUnresolvedInstruction, Opcodes {
  private Number wideValue;
  private boolean resolved;
  private boolean isWide;
  private boolean possiblyNullConst;

  public UnresolvedNumberInsn(boolean wide, long wideValue) {
    super(wideValue);
    if (!wide && ((wideValue & 0xffffffff00000000L) != 0)) {
      throw new IllegalArgumentException("Number is wide but not marked as wide: " + Long.toBinaryString(wideValue));
    }
    if (!wide) {
      cst = Long.valueOf(wideValue).intValue();
    }
    this.isWide = wide;
    this.wideValue = wideValue;
    this.possiblyNullConst = wideValue == 0;
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
    if (resolved) {
      return;
    }
    if (type.getSize() != (isWide ? 2 : 1)) {
      throw new IllegalArgumentException("Wrong size, expected a " + (isWide ? "wide" : "single word") + " type, but got " + type.getClassName());
    }
    switch (type.getSort()) {
    case Type.BOOLEAN:
    case Type.INT:
    case Type.BYTE:
    case Type.SHORT:
    case Type.CHAR:
      cst = wideValue.intValue();
      break;
    case Type.FLOAT:
      cst = Float.intBitsToFloat(wideValue.intValue());
      break;
    case Type.LONG:
      cst = (long) wideValue;
      break;
    case Type.DOUBLE:
      cst = Double.longBitsToDouble(wideValue.longValue());
      break;
    case Type.OBJECT:
    case Type.ARRAY:
      if (possiblyNullConst)
        cst = Type.getType("V"); // TODO: replace afterwards with postoptimizer
      else
        throw new IllegalArgumentException("Expected const 0 for object type, but value is " + cst.toString());
    case Type.VOID:
      throw new IllegalArgumentException("Tried to set illegal type of unresolved number instruction");
    }
    resolved = true;
  }

  @Override
  public boolean isResolved() {
    return resolved;
  }

  @Override
  public boolean tryResolve(int index, MethodNode method, Frame<AbstractValue>[] frames) {
    // as variables are resolved before, we can use them to resolve numbers by their register types
    VarInsnNode store = (VarInsnNode) method.instructions.get(index + 1);
    if (store instanceof UnresolvedVarInsn && !((UnresolvedVarInsn) store).isResolved()) {
      throw new IllegalArgumentException();
    }
    switch (store.getOpcode()) {
    case ASTORE:
      setType(OBJECT_TYPE);
      break;
    case ISTORE:
      setType(Type.INT_TYPE);
      break;
    case FSTORE:
      setType(Type.FLOAT_TYPE);
      break;
    case DSTORE:
      setType(Type.DOUBLE_TYPE);
      break;
    case LSTORE:
      setType(Type.LONG_TYPE);
      break;
    default:
      throw new IllegalArgumentException();
    }

    return true;
  }
}
