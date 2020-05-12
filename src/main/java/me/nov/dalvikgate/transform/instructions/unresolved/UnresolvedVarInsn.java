package me.nov.dalvikgate.transform.instructions.unresolved;

import static me.nov.dalvikgate.utils.UnresolvedUtils.*;

import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import me.coley.analysis.util.FrameUtil;
import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;
import me.nov.dalvikgate.utils.UnresolvedUtils;

public class UnresolvedVarInsn extends VarInsnNode implements IUnresolvedInstruction, Opcodes {
  private final boolean store;
  private final Type initialType;
  private boolean resolvedOp;
  private boolean resolvedVar;

  /**
   * Create new unresolved variable instruction.
   *
   * @param store       {@code true} for storage insns.
   * @param initialType Initial type indicated by android instruction. Will be {@code null} if ambiguous.
   */
  public UnresolvedVarInsn(boolean store, Type initialType) {
    super(UnresolvedUtils.getDefaultVarOp(store), -1);
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
    resolvedVar = true;
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
      throw new IllegalArgumentException("Unsupported var type: " + type.getDescriptor());
    }
    resolvedOp = true;
  }

  public boolean isStore() {
    return store;
  }

  @Override
  public boolean isResolved() {
    return resolvedVar && resolvedOp;
  }

  @Override
  public boolean tryResolve(int index, MethodNode method, Frame<AbstractValue>[] frames) {
    Type retType = null;
    int retIndex = -1;
    for (int j = 0; j < method.instructions.size(); j++) {
      if (j == index)
        continue;
      switch (method.instructions.get(j).getOpcode()) {
      case ARETURN:
      case IRETURN:
      case FRETURN:
      case DRETURN:
      case LRETURN:
        retIndex = j;
        retType = Type.getMethodType(method.desc).getReturnType();
        break;
      case RETURN:
      default:
        break;
      }
    }
    if (retType != null) {
      AbstractValue retValue = FrameUtil.getTopStack(frames[retIndex]);
      if (isDirectlyResponsible(this, retValue)) {
        setType(retType);
        return true;
      }
    }

    // problem here is that AbstractValue can have the value of an unresolved number. Let's say we want a double. UnresolvedNumberInsn uses long as default type. The analyzer takes the
    // long and assumes the var store is lstore.

    // make use of UnresolvedUtils.containsUnresolved(value.getInsns())) here
    if (store) {
      AbstractValue value = FrameUtil.getTopStack(frames[index]);
      setType(value.getType());
    } else {
      AbstractValue local = frames[index].getLocal(var);
      setType(local.getType());
    }
    return isResolved();
  }
}
