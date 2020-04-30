package me.nov.dalvikgate.transform.classes;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedMethodEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedStringEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedTypeEncodedValue;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.fields.FieldTransfomer;
import me.nov.dalvikgate.transform.methods.MethodTransfomer;

public class ClassTransformer implements ITransformer<DexBackedClassDef, ClassNode> {
  private static final int CLASS_V8 = 52;
  private ClassNode cn;

  @Override
  public void build(DexBackedClassDef clazz) {
    cn = new ClassNode();
    cn.name = Type.getType(clazz.getType()).getInternalName();
    cn.version = CLASS_V8;
    cn.access = clazz.getAccessFlags(); // dex uses the same access codes
    cn.interfaces = clazz.getInterfaces().stream()
            .map(itr -> Type.getType(itr).getInternalName())
            .collect(Collectors.toList());
    String superClass = clazz.getSuperclass();
    if (superClass != null)
      cn.superName = Type.getType(superClass).getInternalName();
    else
      cn.superName = "java/lang/Object";
    cn.sourceFile = clazz.getSourceFile();
    // Parse annotations for additional data
    for (DexBackedAnnotation anno : clazz.getAnnotations()) {
      String type = anno.getType();
      if ("Ldalvik/annotation/SourceDebugExtension;".equals(type)) {
        DexBackedStringEncodedValue encodedValue = (DexBackedStringEncodedValue) anno.getElements().iterator().next().getValue();
        cn.sourceDebug = encodedValue.getValue();
      } else if ("Ldalvik/annotation/InnerClass;".equals(type)) {
        int innerAcc = 0;
        String innerType = null;
        for (DexBackedAnnotationElement element : anno.getElements()) {
          if ("accessFlags".equals(element.getName())) {
            innerAcc = (Integer) DexLibCommons.toObject(element.getValue());
          } else if ("name".equals(element.getName())) {
            // Some elements are "null", do we do anything with those?
            if (element.getValue() instanceof DexBackedStringEncodedValue) {
              innerType = (String) DexLibCommons.toObject(element.getValue());
            }
          }
        }
        // Add if type discovered
        if (innerType != null) {
          // Self-reference
          if (cn.name.endsWith("$" + innerType)) {
            // TODO: What if outerClass is not yet parsed? Maybe have a "todo" set of operations that are executed
            // once the necessary data is loaded.
            cn.innerClasses.add(new InnerClassNode(cn.name, cn.outerClass, innerType, innerAcc));
          }
          // True inner
          else {
            cn.innerClasses.add(new InnerClassNode(cn.name + "$" + innerType, cn.name, innerType, innerAcc));
          }
        }
      } else if ("Ldalvik/annotation/MemberClasses;".equals(type)) {
        // TODO: Similar to inner classes? - https://source.android.com/devices/tech/dalvik/dex-format#dalvik-memberclasses
        //
        // array = ((DexBackedArrayEncodedValue)anno.getElements().iterator().next().getValue())
        // for (int i = 0; i < array.size(); i++)
        // String type = ((DexBackedTypeEncodedValue) array.getValue().get(i)).getValue()
      } else if ("Ldalvik/annotation/Signature;".equals(type)) {
        // Signatures stored in an array of strings.
        // Concatting all items will yield the full signature.
        cn.signature = DexLibCommons.arrayToString((DexBackedArrayEncodedValue) anno.getElements().iterator().next().getValue());
      } else if ("Ldalvik/annotation/EnclosingMethod;".equals(type)) {
        DexBackedMethodEncodedValue encodedMethod = (DexBackedMethodEncodedValue) anno.getElements().iterator().next().getValue();
        MethodReference enclosingMethod = encodedMethod.getValue();
        if (cn.outerClass == null) {
          cn.outerClass = Type.getType(enclosingMethod.getDefiningClass()).getInternalName();
        }
        cn.outerMethod = enclosingMethod.getName();
        cn.outerMethodDesc = DexLibCommons.getMethodDesc(enclosingMethod);
      } else if ("Ldalvik/annotation/EnclosingClass;".equals(type)) {
        DexBackedTypeEncodedValue encodedValue = (DexBackedTypeEncodedValue) anno.getElements().iterator().next().getValue();
        cn.outerClass = Type.getType(encodedValue.getValue()).getInternalName();
      } else if ("Lkotlin/Metadata;".equals(type)) {
        // Do nothing
      } else {
        // TODO normal annotations
        // if(anno.getVisibility() == ...)
        if (cn.visibleAnnotations == null) {
          cn.visibleAnnotations = new ArrayList<>();
        }
        cn.visibleAnnotations.add(new AnnotationNode(type));
      }
      // Ignore: Lkotlin/Metadata;
    }
    clazz.getMethods().forEach(this::addMethod);
    clazz.getFields().forEach(this::addField);
  }

  public void addMethod(DexBackedMethod m) {
    MethodTransfomer mt = new MethodTransfomer();
    mt.visit(m);
    cn.methods.add(mt.getTransformed());
  }

  public void addField(DexBackedField f) {
    FieldTransfomer ft = new FieldTransfomer();
    ft.visit(f);
    cn.fields.add(ft.getTransformed());
  }

  @Override
  public ClassNode getTransformed() {
    return Objects.requireNonNull(cn);
  }
}
