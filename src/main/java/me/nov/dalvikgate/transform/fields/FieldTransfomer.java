package me.nov.dalvikgate.transform.fields;

import java.util.*;

import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;

public class FieldTransfomer implements ITransformer<DexBackedField, FieldNode>, Opcodes {
  private FieldNode fn;

  @Override
  public void build(DexBackedField field) {
    String name = field.getName();
    int flags = field.getAccessFlags();
    if (name.startsWith("$$") || name.startsWith("_$_") || name.endsWith("$delegate")) {
      flags |= ACC_SYNTHETIC;
    }
    fn = new FieldNode(flags, name, field.getType(), null, DexLibCommons.toObject(field.getInitialValue()));
    Set<? extends DexBackedAnnotation> annotations = field.getAnnotations();
    if (annotations != null) {
      for (DexBackedAnnotation anno : annotations) {
        String type = anno.getType();
        if ("Ldalvik/annotation/Signature;".equals(type)) {
          // Signatures stored in an array of strings.
          // Concatting all items will yield the full signature.
          fn.signature = DexLibCommons.arrayToString((DexBackedArrayEncodedValue) anno.getElements().iterator().next().getValue());
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
  public FieldNode getTransformed() {
    return Objects.requireNonNull(fn);
  }
}
