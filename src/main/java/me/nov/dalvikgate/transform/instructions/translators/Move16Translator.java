package me.nov.dalvikgate.transform.instructions.translators;

import org.jf.dexlib2.builder.instruction.BuilderInstruction32x;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;

public class Move16Translator extends AbstractInsnTranslator<BuilderInstruction32x> {
  public Move16Translator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction32x i) {
    int source = i.getRegisterB();
    int destination = i.getRegisterA();
    if (source == destination)
      return;
    switch (i.getOpcode()) {
    case MOVE_16:
      addLocalGet(source, false);
      addLocalSet(destination, false);
      break;
    case MOVE_WIDE_16:
      // Cannot determine if double or long, resolve later
      addLocalGet(source, true);
      addLocalSet(destination, true);
      break;
    case MOVE_OBJECT_16:
      addLocalGetObject(source);
      addLocalSetObject(destination);
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}
