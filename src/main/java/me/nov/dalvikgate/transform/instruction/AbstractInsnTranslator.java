package me.nov.dalvikgate.transform.instruction;

import java.util.HashMap;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.Label;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public abstract class AbstractInsnTranslator<T extends BuilderInstruction> implements Opcodes {

  protected InstructionTransformer it;
  protected InsnList il;
  protected HashMap<BuilderInstruction, LabelNode> labels;

  public AbstractInsnTranslator(InstructionTransformer it) {
    this.it = it;
    this.il = it.il;
    this.labels = it.labels;
  }

  /**
   * Translates a DEX instruction to java bytecode and adds it to the list
   * 
   * @param list  The java bytecode instruction list
   * @param input The dex instruction
   */
  public abstract void translate(T input);

  /**
   * Get the assigned ASM LabelNode for a DEX label using the label map generated by {@link #buildLabels() buildLabels}. Multiple labels can have the same LabelNode.
   * 
   * @param label The label
   */
  public LabelNode getASMLabel(Label label) {
    return it.getASMLabel(label);
  }

  /**
   * Method to convert registers to labels, considering parameters and method visibility. Be aware that dalvik reuses unused registers.
   * 
   * @param register The register to be converted
   */
  protected int regToLocal(int register) {
    return it.regToLocal(register);
  }

  /**
   * Gets the next instruction, ignoring payloads
   */
  protected BuilderInstruction getNextOf(BuilderInstruction i) {
    return it.getNextOf(i);
  }

  /**
   * Add local set for object type.
   *
   * @param register Register index.
   */
  protected void addLocalSetObject(int register) {
    it.addLocalSetObject(register);
  }

  /**
   * Add local set for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to put.
   */
  protected void addLocalSet(int register, Type type) {
    it.addLocalSet(register, type);
  }

  /**
   * Add local set for potentially int type.
   *
   * @param register Register index.
   * @param value    Int value.
   */
  protected void addLocalSet(int register, int value) {
    it.addLocalSet(register, value);
  }

  /**
   * Add local set for potentially long type.
   *
   * @param register Register index.
   * @param value    Long value.
   */
  protected void addLocalSet(int register, long value) {
    it.addLocalSet(register, value);
  }

  /**
   * Add local get for object type.
   *
   * @param register Register index.
   */
  protected void addLocalGetObject(int register) {
    it.addLocalGetObject(register);
  }

  /**
   * Add local get for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to get.
   */
  protected void addLocalGet(int register, Type type) {
    it.addLocalGet(register, type);
  }

  /**
   * Add local set for given type.
   *
   * @param store    {@code true} when insn is a setter.
   * @param register Variable index.
   * @param type     Type of variable. {@code null} if ambiguous.
   */
  protected void addLocalGetSet(boolean store, int register, Type type) {
    it.addLocalGetSet(store, register, type);
  }
}
