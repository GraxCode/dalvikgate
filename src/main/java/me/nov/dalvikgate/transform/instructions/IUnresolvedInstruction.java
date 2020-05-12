package me.nov.dalvikgate.transform.instructions;

import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

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
   * @return {@code true} if instruction's value was resolved.
   */
  boolean isResolved();

  /**
   * Try to resolve self given some input.
   *
   * @param index  Instruction index.
   * @param method Method instance.
   * @param frames Frames of method.
   * @return {@code true} when successfully resolved.
   */
  boolean tryResolve(int index, MethodNode method, Frame<AbstractValue>[] frames);
}
