package me.nov.dalvikgate.transform.instruction.translators;

import static org.objectweb.asm.Type.INT_TYPE;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.objectweb.asm.tree.JumpInsnNode;

import me.nov.dalvikgate.transform.instruction.IInsnTranslator;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instruction.tree.UnresolvedJumpInsnNode;

public class F21tTranslator extends IInsnTranslator<BuilderInstruction21t> {

  public F21tTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction21t i) { 
    // Branch to the given destination if the given register's value compares with 0 as specified.
    // A: register to test (8 bits)
    // B: signed branch offset (16 bits)
    int source = i.getRegisterA();
    Opcode opcode = i.getOpcode();
    boolean refsMustBeNumeric = !(opcode == Opcode.IF_EQZ || opcode == Opcode.IF_NEZ);
    if (refsMustBeNumeric) {
      // TODO: can single jumps in dalvik also take longs, doubles, etc?
      // - Or are all single jumps based on integers?
      addLocalGet(source, INT_TYPE);
    } else {
      addLocalGet(source, null);
    }
    Label label = i.getTarget();
    switch (opcode) {
    case IF_EQZ:
    case IF_NEZ:
      // Dalvik has no "null", instead used "0" which means we can't immediately tell if we should use object comparison, or integer comparison.
      // So we will resolve the opcode later when we can perform type analysis.
      il.add(new UnresolvedJumpInsnNode(opcode, getASMLabel(label)));
      break;
    case IF_LTZ:
      il.add(new JumpInsnNode(IFLT, getASMLabel(label)));
      break;
    case IF_GEZ:
      il.add(new JumpInsnNode(IFGE, getASMLabel(label)));
      break;
    case IF_GTZ:
      il.add(new JumpInsnNode(IFGT, getASMLabel(label)));
      break;
    case IF_LEZ:
      il.add(new JumpInsnNode(IFLE, getASMLabel(label)));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}