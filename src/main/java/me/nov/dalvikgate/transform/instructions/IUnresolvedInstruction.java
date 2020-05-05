package me.nov.dalvikgate.transform.instructions;

import org.objectweb.asm.Type;

public interface IUnresolvedInstruction {
  /**
   * Validate the opcode is set.
   */
  void validate();

  /**
   * Update the instruction's type to determine its opcode.
   *
   * @param type Type discovered by analyzer.
   */
  void setType(Type type);
}
