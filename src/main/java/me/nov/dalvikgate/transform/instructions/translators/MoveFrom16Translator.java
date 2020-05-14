package me.nov.dalvikgate.transform.instructions.translators;

import org.jf.dexlib2.builder.instruction.BuilderInstruction22x;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;

public class MoveFrom16Translator extends AbstractInsnTranslator<BuilderInstruction22x> {

  public MoveFrom16Translator(InstructionTransformer it) {
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
      addLocalGet(source, false);
      addLocalSet(destination, false);
      break;
    case MOVE_WIDE_FROM16:
      // Cannot determine if double or long, resolve later
      addLocalGet(source, true);
      addLocalSet(destination, true);
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
