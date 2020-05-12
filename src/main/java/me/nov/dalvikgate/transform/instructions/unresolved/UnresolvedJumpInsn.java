package me.nov.dalvikgate.transform.instructions.unresolved;

import java.util.Map;

import me.nov.dalvikgate.utils.UnresolvedUtils;
import org.jf.dexlib2.Opcode;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;

public class UnresolvedJumpInsn extends JumpInsnNode implements IUnresolvedInstruction, Opcodes {
  private final Opcode dalvikOp;
  private boolean resolved;

  /**
   * Create new unresolved jump instruction.
   *
   * @param dalvikOp Original dalvik opcode.
   * @param label    Destination of jump
   */
  public UnresolvedJumpInsn(Opcode dalvikOp, LabelNode label) {
    super(UnresolvedUtils.getDefaultJumpOp(dalvikOp), label);
    this.dalvikOp = dalvikOp;
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

  public void validate() {
    if (DexToASM.noResolve)
      setType(Type.getType("Ljava/lang/Object;"));
    if (opcode < 0)
      throw new UnresolvedInsnException("Jump opcode has not been resolved!");
  }

  public void setType(Type type) {
    boolean isObject = type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    switch (dalvikOp) {
    case IF_EQ:
      opcode = isObject ? IF_ACMPEQ : IF_ICMPEQ;
      break;
    case IF_NE:
      opcode = isObject ? IF_ACMPNE : IF_ICMPNE;
      break;
    case IF_EQZ:
      opcode = isObject ? IFNULL : IFEQ;
      break;
    case IF_NEZ:
      opcode = isObject ? IFNONNULL : IFNE;
      break;
    default:
      throw new IllegalStateException("Jump cannot be unresolved! Got " + dalvikOp.name);
    }
    resolved = true;
  }

  @Override
  public boolean isResolved() {
    return resolved;
  }
}
