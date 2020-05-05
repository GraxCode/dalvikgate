package me.nov.dalvikgate.transform.instructions.translators.references;

import static me.nov.dalvikgate.asm.ASMCommons.*;
import static org.objectweb.asm.Type.*;

import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c;
import org.jf.dexlib2.iface.reference.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;


public class F22cTranslator extends AbstractInsnTranslator<BuilderInstruction22c> {

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
      if (i.getOpcode().name.startsWith("iget")) { // ugly, i know
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