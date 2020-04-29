package me.nov.dalvikgate.transform.fields;

import org.jf.dexlib2.dexbacked.DexBackedField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.values.ValueBridge;

public class FieldTransfomer implements ITransformer<FieldNode>, Opcodes {

  private final DexBackedField field;
  private FieldNode fn;

  public FieldTransfomer(DexBackedField field) {
    this.field = field;
  }

  @Override
  public void build() {
    String name = field.getName();
    int flags = field.getAccessFlags();
    if (name.startsWith("$$") || name.startsWith("_$_") || name.endsWith("$delegate")) {
      flags |= ACC_SYNTHETIC;
    }
    fn = new FieldNode(flags, name, field.getType(), null, ValueBridge.toObject(field.getInitialValue()));
    // TODO annotations
  }

  @Override
  public FieldNode get() {
    return fn;
  }

}
