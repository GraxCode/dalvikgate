package me.nov.dalvikgate.transform.instructions.exception;

import org.jf.dexlib2.builder.BuilderInstruction;

public class UnsupportedInsnException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UnsupportedInsnException(BuilderInstruction insn) {
    super("Unsupported instruction: " + insn.getOpcode().name + " (Format: " + insn.getFormat().name() + ") (Class: " + insn.getClass().getSimpleName() + ")");
  }

  public UnsupportedInsnException(String detail, BuilderInstruction insn) {
    super("Unsupported instruction: " + insn.getOpcode().name + " (Format: " + insn.getFormat().name() + ") - " + detail);
  }
}
