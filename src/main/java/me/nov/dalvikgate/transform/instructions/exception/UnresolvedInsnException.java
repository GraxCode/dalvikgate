package me.nov.dalvikgate.transform.instructions.exception;

public class UnresolvedInsnException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UnresolvedInsnException(String message) {
    super(message);
  }
}
