package me.nov.dalvikgate.transform.instruction.tree;

import me.nov.dalvikgate.transform.instruction.exception.UnresolvedInsnException;
import org.jf.dexlib2.Opcode;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Map;

public class UnresolvedJumpInsnNode extends JumpInsnNode implements Opcodes {
  private final Opcode dalvikOp;

  /**
   * Create new unresolved jump instruction.
   *
   * @param label  the operand of the instruction to be constructed. This operand is a label that
   */
  public UnresolvedJumpInsnNode(Opcode dalvikOp, LabelNode label) {
    super(-1, label);
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

  /**
   * Validate the opcode is set.
   */
  private void validate() {
    if (opcode < 0)
      throw new UnresolvedInsnException("Variable opcode has not been resolved!");
  }

  /**
   * Update opcode with knowledge of if the type being operated on is an object.
   *
   * @param isObject {@code true} when type of jump comparison is an object.
   */
  public void setIsObject(boolean isObject) {
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
        throw new IllegalStateException("Unsupported opcode type: " + dalvikOp.name);
      }
  }
}
