package me.nov.dalvikgate.utils;

import java.util.*;
import java.util.stream.Collectors;

import org.jf.dexlib2.iface.reference.MethodReference;
import org.objectweb.asm.Type;

public class CustomMethodReference implements MethodReference {

  private String defClass;
  private String name;
  private List<? extends CharSequence> params;
  private String returnType;

  public CustomMethodReference(String defClass, String name, List<? extends CharSequence> params, String returnType) {
    super();
    this.defClass = defClass;
    this.name = name;
    this.params = params;
    this.returnType = returnType;
  }

  public CustomMethodReference(String defClass, String name, String desc) {
    super();
    this.defClass = defClass;
    this.name = name;
    this.returnType = Type.getMethodType(desc).getReturnType().getDescriptor();
    this.params = Arrays.stream(Type.getMethodType(desc).getArgumentTypes()).map(t -> t.getDescriptor()).collect(Collectors.toList());
  }

  @Override
  public void validateReference() throws InvalidReferenceException {
  }

  @Override
  public String getDefiningClass() {
    return defClass;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<? extends CharSequence> getParameterTypes() {
    return params;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public int compareTo(MethodReference o) {
    return 0;
  }

}
