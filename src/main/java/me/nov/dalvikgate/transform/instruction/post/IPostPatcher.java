package me.nov.dalvikgate.transform.instruction.post;

/**
 * Add patches to make the final code more readable.
 */
public interface IPostPatcher<MethodNode> {
  void applyPatch(MethodNode mn);
}
