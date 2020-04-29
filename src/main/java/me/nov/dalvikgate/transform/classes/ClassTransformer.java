package me.nov.dalvikgate.transform.classes;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedMethodEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedStringEncodedValue;
import org.jf.dexlib2.dexbacked.value.DexBackedTypeEncodedValue;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.IntEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableNullEncodedValue;
import org.jf.dexlib2.util.ReferenceUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.fields.FieldTransfomer;
import me.nov.dalvikgate.transform.methods.MethodTransfomer;
import org.objectweb.asm.tree.InnerClassNode;

public class ClassTransformer implements ITransformer<ClassNode> {

  private final DexBackedClassDef clazz;
  private final int version;
  private ClassNode cn;

  public ClassTransformer(DexBackedClassDef clazz, int version) {
    this.clazz = clazz;
    this.version = version;
  }

  @Override
  public void build() {
    cn = new ClassNode();

    cn.name = Type.getType(clazz.getType()).getInternalName();
    cn.version = version;
    cn.access = clazz.getAccessFlags(); // dex uses the same access codes

    cn.interfaces = new ArrayList<>();
    clazz.getInterfaces().forEach(itr -> cn.interfaces.add(Type.getType(itr).getInternalName()));

    String superClass = clazz.getSuperclass();
    if (superClass != null)
      cn.superName = Type.getType(superClass).getInternalName();
    else
      cn.superName = "java/lang/Object";

    cn.sourceFile = clazz.getSourceFile();

    Set<? extends DexBackedAnnotation> annotations = clazz.getAnnotations();
    if (annotations != null && !annotations.isEmpty()) {
      for (DexBackedAnnotation anno : annotations) {
        String type = anno.getType();
        if ("Ldalvik/annotation/SourceDebugExtension;".equals(type)) {
          DexBackedStringEncodedValue encodedValue = (DexBackedStringEncodedValue) anno.getElements().iterator().next().getValue();
          cn.sourceDebug = encodedValue.getValue();
        } else if ("Ldalvik/annotation/InnerClass;".equals(type)) {
          int innerAcc = 0;
          String innerType = null;
          for (DexBackedAnnotationElement element : anno.getElements()) {
            if ("accessFlags".equals(element.getName())) {
              innerAcc = ((IntEncodedValue) element.getValue()).getValue();
            } else if ("name".equals(element.getName())) {
              // Some elements are "null", do we do anything with those?
              if (element.getValue() instanceof DexBackedStringEncodedValue) {
                innerType = ((DexBackedStringEncodedValue)element.getValue()).getValue();
              }
            }
          }
          // Add if type discoered
          if (innerType != null) {
            // Self-reference
            if (cn.name.endsWith("$" + innerType)) {
              // TODO: What if outerClass is not yet parsed? Maybe have a "todo" set of operations that are executed
              //   once the necessary data is loaded.
              cn.innerClasses.add(new InnerClassNode(cn.name, cn.outerClass, innerType, innerAcc));
            }
            // True inner
            else {
              cn.innerClasses.add(new InnerClassNode(cn.name + "$" + innerType, cn.name, innerType, innerAcc));
            }
          }
        } else if ("Ldalvik/annotation/MemberClasses;".equals(type)) {
          // Similar to inner classes? - https://source.android.com/devices/tech/dalvik/dex-format#dalvik-memberclasses
          //
          // array = ((DexBackedArrayEncodedValue)anno.getElements().iterator().next().getValue())
          // for (int i = 0; i < array.size(); i++)
          // String type = ((DexBackedTypeEncodedValue) array.getValue().get(i)).getValue()
        } else if ("Ldalvik/annotation/Signature;".equals(type)) {
          // Signatures stored in an array of strings.
          // Concatting all items will yield the full signature.
          StringBuilder signature = new StringBuilder();
          DexBackedArrayEncodedValue value = (DexBackedArrayEncodedValue) anno.getElements().iterator().next().getValue();
          for (EncodedValue arrayEntryValue : ((DexBackedArrayEncodedValue) value).getValue()) {
            signature.append(((DexBackedStringEncodedValue) arrayEntryValue).getValue());
          }
          cn.signature = signature.toString();
        } else if ("Lkotlin/Metadata;".equals(type)) {
          // Do nothing
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
        } else {
          System.getProperty("user.dir");
        }
        // Ignore: Ldalvik/annotation/SourceDebugExtension;
        // Ignore: Lkotlin/Metadata;
      }
      /**
       * TODO transform
       * 
       * @EnclosingMethod
       * @InnerClass
       */
//			cn.visibleAnnotations = new ArrayList<>();
//			clazz.getAnnotations().forEach(a -> cn.visibleAnnotations.add(new AnnotationNode(a.getType())));
    }

    clazz.getMethods().forEach(this::addMethod);
    clazz.getFields().forEach(this::addField);
  }

  public void addMethod(DexBackedMethod m) {
    MethodTransfomer mt = new MethodTransfomer(m);
    mt.build();
    cn.methods.add(mt.get());
  }

  public void addField(DexBackedField f) {
    FieldTransfomer ft = new FieldTransfomer(f);
    ft.build();
    cn.fields.add(ft.get());
  }

  @Override
  public ClassNode get() {
    return Objects.requireNonNull(cn);
  }
}
