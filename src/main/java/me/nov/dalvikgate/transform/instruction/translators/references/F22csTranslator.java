package me.nov.dalvikgate.transform.instruction.translators.references;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;
import me.nov.dalvikgate.utils.CustomFieldReference;

public class F22csTranslator extends AbstractInsnTranslator<BuilderInstruction22cs> {

  public F22csTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction22cs i) {
    // we can't really translate this to java, as it uses the current object on stack as class, and performs field action at index
    // TODO this could be translated to reflection, but would be a lot of work
    new F22cTranslator(it).translate(new BuilderInstruction22c(convertOpcode(i), i.getRegisterA(), i.getRegisterB(),
        new CustomFieldReference("Ljava/lang/Object;", "$field_with_offset_" + i.getFieldOffset(), getSampleTypeForOp(i.getOpcode()))));
  }

  private String getSampleTypeForOp(Opcode opcode) {
    switch (opcode) {
    case IPUT_QUICK:
    case IGET_QUICK:
      return "I";
    case IPUT_WIDE_QUICK:
    case IGET_WIDE_QUICK:
      return "J"; // can be D too
    default:
    case IPUT_OBJECT_QUICK:
    case IGET_OBJECT_QUICK:
      return "Ljava/lang/Object";
    case IPUT_BOOLEAN_QUICK:
    case IGET_BOOLEAN_QUICK:
      return "Z";
    case IPUT_BYTE_QUICK:
    case IGET_BYTE_QUICK:
      return "B";
    case IPUT_CHAR_QUICK:
    case IGET_CHAR_QUICK:
      return "C";
    case IPUT_SHORT_QUICK:
    case IGET_SHORT_QUICK:
      return "S";
    }
  }

  private Opcode convertOpcode(BuilderInstruction22cs i) {
    switch (i.getOpcode()) {
    case IGET_QUICK:
      return Opcode.IGET;
    case IGET_WIDE_QUICK:
      return Opcode.IGET_WIDE;
    case IGET_OBJECT_QUICK:
      return Opcode.IGET_OBJECT;
    case IPUT_QUICK:
      return Opcode.IPUT;
    case IPUT_WIDE_QUICK:
      return Opcode.IPUT_WIDE;
    case IPUT_OBJECT_QUICK:
      return Opcode.IPUT_OBJECT;
    case IPUT_BOOLEAN_QUICK:
      return Opcode.IPUT_BOOLEAN;
    case IPUT_BYTE_QUICK:
      return Opcode.IPUT_BYTE;
    case IPUT_CHAR_QUICK:
      return Opcode.IPUT_CHAR;
    case IPUT_SHORT_QUICK:
      return Opcode.IPUT_SHORT;
    case IGET_BOOLEAN_QUICK:
      return Opcode.IGET_BOOLEAN;
    case IGET_BYTE_QUICK:
      return Opcode.IGET_BYTE;
    case IGET_CHAR_QUICK:
      return Opcode.IGET_CHAR;
    case IGET_SHORT_QUICK:
      return Opcode.IGET_SHORT;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}
