package me.nov.dalvikgate.transform.classes;

import java.util.ArrayList;
import java.util.Objects;

import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.fields.FieldTransfomer;
import me.nov.dalvikgate.transform.methods.MethodTransfomer;

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

    cn.interfaces = new ArrayList<>(clazz.getInterfaces());
    cn.superName = clazz.getSuperclass();

    cn.sourceFile = clazz.getSourceFile();

    if (clazz.getAnnotations() != null) {
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
