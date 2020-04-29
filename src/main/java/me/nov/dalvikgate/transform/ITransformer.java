package me.nov.dalvikgate.transform;

public interface ITransformer<T> {
  public void build();

  public T get();
}
