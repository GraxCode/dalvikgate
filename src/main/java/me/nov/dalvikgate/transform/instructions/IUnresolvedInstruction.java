package me.nov.dalvikgate.transform.instructions;

import org.objectweb.asm.Type;

/**
 * A node that represents an instruction that has not been fully resolved. Due to the ambiguity of android bytecode certain actions are not immediately clear in the same way they
 * would be in plain Java bytecode.
 */
public interface IUnresolvedInstruction {
  /**
   * Validate the opcode is set. Should throw an exception if setType hasn't been invoked.
   */
  void validate();

  /**
   * Update the instruction's type to determine its opcode and value.
   *
   * @param type Type discovered by analyzer.
   */
  void setType(Type type);

  /**
   * @return type if resolved.
   */
  Type getResolvedType();

  /**
   * @return {@code true} if instruction's value was resolved.
   */
  boolean isResolved();
}
