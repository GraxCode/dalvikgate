package me.nov.dalvikgate.transform.instructions.translators.references;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;
import org.objectweb.asm.Type;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;

import me.nov.dalvikgate.utils.CustomFieldReference;

public class QuickFieldTranslator extends AbstractInsnTranslator<BuilderInstruction22cs> {

  public QuickFieldTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction22cs i) {
    // we can't really translate this to java, as it uses the current object on stack as class, and performs field action at index
    // TODO this could be translated to reflection, but would be a lot of work
    new F22cTranslator(it).translate(new BuilderInstruction22c(convertOpcode(i), i.getRegisterA(), i.getRegisterB(),
        new CustomFieldReference("Ljava/lang/Object;", "$$$field_offset_" + i.getFieldOffset(), getSampleTypeForOp(i.getOpcode()))));
  }

  private String getSampleTypeForOp(Opcode opcode) {
    switch (opcode) {
    case IPUT_QUICK:
    case IGET_QUICK:
      return Type.INT_TYPE.getDescriptor();
    case IPUT_WIDE_QUICK:
    case IGET_WIDE_QUICK:
      return Type.LONG_TYPE.getDescriptor(); // can be D too
    default:
    case IPUT_OBJECT_QUICK:
    case IGET_OBJECT_QUICK:
      return OBJECT_TYPE.getDescriptor();
    case IPUT_BOOLEAN_QUICK:
    case IGET_BOOLEAN_QUICK:
      return Type.BOOLEAN_TYPE.getDescriptor();
    case IPUT_BYTE_QUICK:
    case IGET_BYTE_QUICK:
      return Type.BYTE_TYPE.getDescriptor();
    case IPUT_CHAR_QUICK:
    case IGET_CHAR_QUICK:
      return Type.CHAR_TYPE.getDescriptor();
    case IPUT_SHORT_QUICK:
    case IGET_SHORT_QUICK:
      return Type.SHORT_TYPE.getDescriptor();
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
