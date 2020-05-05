package me.nov.dalvikgate.transform.instruction.translators;

import static org.objectweb.asm.Type.*;

import org.jf.dexlib2.builder.instruction.BuilderInstruction22x;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F22xTranslator extends AbstractInsnTranslator<BuilderInstruction22x> {

  public F22xTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction22x i) {
    int source = i.getRegisterB();
    int destination = i.getRegisterA();
    if (source == destination)
      return;
    switch (i.getOpcode()) {
    case MOVE_FROM16:
      // TODO can be float too
      addLocalGet(source, INT_TYPE);
      addLocalSet(destination, INT_TYPE);
      break;
    case MOVE_WIDE_FROM16:
      // Cannot determine if double or long, resolve later
      addLocalGet(source, null);
      addLocalSet(destination, null);
      break;
    case MOVE_OBJECT_FROM16:
      addLocalGetObject(source);
      addLocalSetObject(destination);
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}
