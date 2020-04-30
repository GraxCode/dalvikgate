package me.nov.dalvikgate.transform.instruction.exception;

import org.jf.dexlib2.builder.BuilderInstruction;

public class UnsupportedInsnException extends RuntimeException {
  public UnsupportedInsnException(BuilderInstruction insn) {
    super("Unsupported instruction: " + insn.getOpcode().name + " (Format: " + insn.getFormat().name() + ") (Class: " + insn.getClass().getSimpleName() + ")");
  }

  public UnsupportedInsnException(String detail, BuilderInstruction insn) {
    super("Unsupported instruction: " + insn.getOpcode().name + " (Format: " + insn.getFormat().name() + ") - " + detail);
  }
}
