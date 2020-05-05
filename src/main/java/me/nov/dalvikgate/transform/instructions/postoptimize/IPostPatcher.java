package me.nov.dalvikgate.transform.instructions.postoptimize;

/**
 * Add patches to make the final code more readable.
 */
public interface IPostPatcher<MethodNode> {
  void applyPatch(MethodNode mn);
}
