package me.nov.dalvikgate.transform.instruction.translators;

import static me.nov.dalvikgate.asm.ASMCommons.ARRAY_TYPE;
import static me.nov.dalvikgate.asm.ASMCommons.getPrimitiveIndex;
import static me.nov.dalvikgate.asm.ASMCommons.getTypeForDesc;
import static org.objectweb.asm.Type.INT_TYPE;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.nov.dalvikgate.transform.instruction.IInsnTranslator;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F22cTranslator extends IInsnTranslator<BuilderInstruction22c> {

  public F22cTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction22c i) {
    if (i.getReferenceType() == ReferenceType.FIELD) {
      // non-static field ops
      FieldReference fieldReference = (FieldReference) i.getReference();
      String owner = Type.getType(fieldReference.getDefiningClass()).getInternalName();
      String name = fieldReference.getName();
      String desc = fieldReference.getType();
      if (i.getOpcode().name.startsWith("iget")) {
        // B: object reference
        // A: destination
        addLocalGetObject(i.getRegisterB());
        il.add(new FieldInsnNode(GETFIELD, owner, name, desc));
        addLocalSet(i.getRegisterA(), Type.getType(desc));
      } else {
        // B: object reference
        // A: value
        addLocalGetObject(i.getRegisterB());
        addLocalGet(i.getRegisterA(), getTypeForDesc(desc));
        il.add(new FieldInsnNode(PUTFIELD, owner, name, desc));
      }
    } else {
      TypeReference typeReference = (TypeReference) i.getReference();
      if (i.getOpcode() == Opcode.INSTANCE_OF) {
        addLocalGetObject(i.getRegisterB()); // object reference
        il.add(new TypeInsnNode(INSTANCEOF, Type.getType(typeReference.getType()).getInternalName()));
        addLocalSet(i.getRegisterA(), INT_TYPE); // boolean
      } else if (i.getOpcode() == Opcode.NEW_ARRAY) {
        addLocalGet(i.getRegisterB(), INT_TYPE); // array size
        Type arrayType = Type.getType(typeReference.getType()).getElementType();
        if (arrayType.getSort() == Type.OBJECT) {
          il.add(new TypeInsnNode(ANEWARRAY, arrayType.getInternalName()));
        } else {
          il.add(new IntInsnNode(NEWARRAY, getPrimitiveIndex(arrayType.getDescriptor())));
        }
        addLocalSet(i.getRegisterA(), ARRAY_TYPE);
      } else {
        throw new UnsupportedInsnException(i);
      }
    }
  }
}