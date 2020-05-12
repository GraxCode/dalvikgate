package me.nov.dalvikgate.transform.instructions.translators.jump;

import static org.objectweb.asm.Type.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22t;
import org.objectweb.asm.tree.JumpInsnNode;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedJumpInsn;

public class CompareJumpTranslator extends AbstractInsnTranslator<BuilderInstruction22t> {

  public CompareJumpTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction22t i) {
    // Branch to the given destination if the given two registers' values compare as specified.
    // A: first register to test (4 bits)
    // B: second register to test (4 bits)
    int first = i.getRegisterA();
    int second = i.getRegisterB();
    // Check if it we can safely assume the values are numeric.
    // - Any compare op except <IF_EQ> and <IF_NE> is safely numeric
    Opcode opcode = i.getOpcode();
    boolean refsMustBeNumeric = !(opcode == Opcode.IF_EQ || opcode == Opcode.IF_NE);
    if (refsMustBeNumeric) {
      // TODO: can comparison jumps in dalvik also take longs, doubles, etc?
      // - Or are all comparison-jumps based on integers?
      addLocalGet(first, INT_TYPE);
      addLocalGet(second, INT_TYPE);
    } else {
      addLocalGet(first, null);
      addLocalGet(second, null);
    }
    Label label = i.getTarget();
    switch (opcode) {
    case IF_EQ:
    case IF_NE:
      // Dalvik has no "null", instead uses "0" which means we can't immediately tell if we should use object comparison, or integer comparison.
      // So we will resolve the opcode later when we can perform type analysis.
      il.add(new UnresolvedJumpInsn(opcode, toASMLabel(label)));
      break;
    case IF_LT:
      il.add(new JumpInsnNode(IF_ICMPLT, toASMLabel(label)));
      break;
    case IF_GE:
      il.add(new JumpInsnNode(IF_ICMPGE, toASMLabel(label)));
      break;
    case IF_GT:
      il.add(new JumpInsnNode(IF_ICMPGT, toASMLabel(label)));
      break;
    case IF_LE:
      il.add(new JumpInsnNode(IF_ICMPLE, toASMLabel(label)));
      break;
    default:
      throw new UnsupportedInsnException("compare-jump", i);
    }
  }
}