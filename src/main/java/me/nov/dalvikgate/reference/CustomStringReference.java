package me.nov.dalvikgate.reference;

import org.jf.dexlib2.iface.reference.StringReference;

public class CustomStringReference implements StringReference {

  private String str;

  public CustomStringReference(String str) {
    this.str = str;
  }

  @Override
  public void validateReference() throws InvalidReferenceException {
  }

  @Override
  public int length() {
    return str.length();
  }

  @Override
  public char charAt(int index) {
    return str.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return str.subSequence(start, end);
  }

  @Override
  public String getString() {
    return str;
  }

  @Override
  public int compareTo(CharSequence o) {
    return str.compareTo((String) o);
  }
  
  @Override
  public String toString() {
    return str;
  }
}
