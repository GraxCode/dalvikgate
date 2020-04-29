package me.nov.dalvikgate.transform.fields;

import java.util.ArrayList;
import java.util.Set;

import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
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
    Set<? extends DexBackedAnnotation> annotations = field.getAnnotations();
    if (annotations != null) {
      for (DexBackedAnnotation anno : annotations) {
        String type = anno.getType();
        if ("Ldalvik/annotation/Signature;".equals(type)) {
          // Signatures stored in an array of strings.
          // Concatting all items will yield the full signature.
          fn.signature = ValueBridge.arrayToString((DexBackedArrayEncodedValue) anno.getElements().iterator().next().getValue());
        } else if (type != null) {
          // TODO normal annotations
          // if(anno.getVisibility() == ...)
          if (fn.visibleAnnotations == null) {
            fn.visibleAnnotations = new ArrayList<>();
          }
          fn.visibleAnnotations.add(new AnnotationNode(type));
        }
      }
    }
  }

  @Override
  public FieldNode get() {
    return fn;
  }

}
