package me.nov.dalvikgate.reference;

import org.jf.dexlib2.iface.reference.FieldReference;

public class CustomFieldReference implements FieldReference {

  private String defClass;
  private String name;
  private String type;

  public CustomFieldReference(String defClass, String name, String type) {
    if (!defClass.endsWith(";") || !defClass.startsWith("L"))
      throw new IllegalArgumentException();
    this.defClass = defClass;
    this.name = name;
    this.type = type;
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
  public String getType() {
    return type;
  }

  @Override
  public int compareTo(FieldReference o) {
    return 0;
  }
}
