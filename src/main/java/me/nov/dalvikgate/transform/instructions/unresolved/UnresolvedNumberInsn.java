package me.nov.dalvikgate.transform.instructions.unresolved;

import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;

public class UnresolvedNumberInsn extends LdcInsnNode implements IUnresolvedInstruction, Opcodes {
  private Number wideValue;
  private boolean resolved;
  private boolean wide;
  private boolean possiblyNullConst;
  private Type type;

  public UnresolvedNumberInsn(boolean wide, long wideValue) {
    super(wideValue);
    if (!wide) {
      // don't check if wideValue has upper bits, as values can be negative too. dexlib already does checking.
      cst = ((int) wideValue);
    }
    this.wide = wide;
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
    if (type.getSize() != (wide ? 2 : 1)) {
      throw new IllegalArgumentException("Wrong size, expected a " + (wide ? "wide" : "single word") + " type, but got " + type.getClassName());
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
      cst = wideValue.longValue();
      break;
    case Type.DOUBLE:
      cst = Double.longBitsToDouble(wideValue.longValue());
      break;
    case Type.OBJECT:
    case Type.ARRAY:
      if (possiblyNullConst)
        cst = Type.getType("V"); // is replaced afterwards with aconst_null
      else
        throw new IllegalArgumentException("Expected const 0 for object type, but value is " + cst.toString());
      break;
    case Type.VOID:
      throw new IllegalArgumentException("Tried to set illegal type of unresolved number instruction");
    }
    this.type = type;
  }

  @Override
  public boolean isResolved() {
    return type != null;
  }

  public boolean isWide() {
    return wide;
  }

  public boolean isPossiblyNullConst() {
    return possiblyNullConst;
  }

  @Override
  public Type getResolvedType() {
    return type;
  }
}
