package me.nov.dalvikgate.transform.instruction.translators;

import static org.objectweb.asm.Type.INT_TYPE;

import org.jf.dexlib2.builder.instruction.BuilderInstruction22s;
import org.objectweb.asm.tree.InsnNode;

import me.nov.dalvikgate.transform.instruction.IInsnTranslator;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F22sTranslator extends IInsnTranslator<BuilderInstruction22s> {

  public F22sTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction22s i) {
    // Perform the indicated binary op on the indicated register (first argument) and literal value (second argument),
    // storing the result in the destination register.
    // A: destination register (4 bits)
    // B: source register (4 bits)
    // C: signed int constant (16 bits)
    addLocalGet(i.getRegisterB(), INT_TYPE);
    addLocalGet(i.getRegisterA(), INT_TYPE);
    switch (i.getOpcode()) {
    case ADD_INT_LIT16:
      il.add(new InsnNode(IADD));
      break;
    case MUL_INT_LIT16:
      il.add(new InsnNode(IMUL));
      break;
    case DIV_INT_LIT16:
      il.add(new InsnNode(IDIV));
      break;
    case REM_INT_LIT16:
      il.add(new InsnNode(IREM));
      break;
    case AND_INT_LIT16:
      il.add(new InsnNode(IAND));
      break;
    case OR_INT_LIT16:
      il.add(new InsnNode(IOR));
      break;
    case XOR_INT_LIT16:
      il.add(new InsnNode(IXOR));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
    addLocalSet(i.getRegisterA(), INT_TYPE);
  }
}