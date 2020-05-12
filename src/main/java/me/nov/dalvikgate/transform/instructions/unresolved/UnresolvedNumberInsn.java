package me.nov.dalvikgate.transform.instructions.unresolved;

import java.util.List;
import java.util.Map;

import me.coley.analysis.util.FrameUtil;
import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.asm.ASMCommons;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;
import org.objectweb.asm.tree.analysis.Frame;

public class UnresolvedNumberInsn extends LdcInsnNode implements IUnresolvedInstruction, Opcodes {
  private Number wideValue;
  private boolean resolved;
  private boolean isWide;
  private boolean possiblyNullConst;

  public UnresolvedNumberInsn(boolean wide, long wideValue) {
    super(wide ? wideValue : (int) wideValue);
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
        throw new IllegalArgumentException("Type cannot be changed locally, replace this instruction with aconst_null instead.");
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
    // TODO: There has to be a better way...
    // Check for usage:
    // - Field set
    // - Method arg
    // - Variable use in resolved variable
    for (int j = 0; j < method.instructions.size(); j++) {
      AbstractInsnNode insn = method.instructions.get(j);
      int op = insn.getOpcode();
      // Check against fields
      if (insn instanceof FieldInsnNode) {
        // Check if the value being stored in the field
        if ((op == PUTFIELD || op == PUTSTATIC) ) {
          AbstractValue value = FrameUtil.getTopStack(frames[j]);
          if (isDirectlyResponsible(value)) {
            setType(Type.getType(((FieldInsnNode) insn).desc));
            break;
          }
        }
        // Check if the field context is this instruction
        if (op == PUTFIELD || op == GETFIELD) {
          AbstractValue owner = FrameUtil.getStackFromTop(frames[j], 1);
          if (isDirectlyResponsible(owner)) {
            setType(Type.getType(((FieldInsnNode) insn).owner));
            break;
          }
        }
      }
      // Check against method descriptors
      else if (insn instanceof MethodInsnNode) {
        Type methodType = Type.getMethodType(((MethodInsnNode) insn).desc);
        // Check against argument types
        int argCount = methodType.getArgumentTypes().length;
        for (int a = 0; a < argCount; a++) {
          AbstractValue value = FrameUtil.getStackFromTop(frames[j], (argCount - 1) - a);
          if (isDirectlyResponsible(value)) {
            setType(methodType.getArgumentTypes()[a]);
            return true;
          }
        }
        // Check if the method context is this instruction
        if (op == INVOKEINTERFACE || op == INVOKESPECIAL || op == INVOKEVIRTUAL) {
          AbstractValue owner = FrameUtil.getStackFromTop(frames[j], 1);
          if (isDirectlyResponsible(owner)) {
            setType(Type.getType(((MethodInsnNode) insn).owner));
            break;
          }
        }
      }
      // Check against variables
      else if (insn instanceof VarInsnNode) {
        // Resolvable variables must be resolved first
        if (insn instanceof UnresolvedVarInsn) {
          UnresolvedVarInsn unresolvedVarInsn = (UnresolvedVarInsn) insn;
          if (unresolvedVarInsn.isResolved()) {
            setType(ASMCommons.getPushedTypeForInsn(insn));
            break;
          }
        }
        // Normal variable
        else {
          setType(ASMCommons.getPushedTypeForInsn(insn));
          break;
        }
      }
      // Check against zero-operand instructions (math types and such)
      else if (insn instanceof InsnNode) {
        AbstractValue value = FrameUtil.getTopStack(frames[j]);
        if (isDirectlyResponsible(value)) {
          Type type = ASMCommons.getOperatingType((InsnNode) insn);
          setType(type);
        }
      }
    }
    return false;
  }

  /**
   * Check if the instruction immediately given in the contributing insns list is this current insn... unless its a variable load.
   * Then check the next insn.
   *
   * @param value Value to check.
   * @return {@code true} if this instruction is responsible for the given value.
   */
  private boolean isDirectlyResponsible(AbstractValue value) {
    List<AbstractInsnNode> insns = value.getInsns();
    AbstractInsnNode top;
    int offset = 1;
    while ((top = insns.get(insns.size() - offset)) instanceof VarInsnNode)
      offset++;
    return equals(top);
  }
}
