package me.nov.dalvikgate.transform.instruction.translators;

import static me.nov.dalvikgate.asm.ASMCommons.OBJECT_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;

import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.nov.dalvikgate.transform.instruction.IInsnTranslator;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F21cTranslator extends IInsnTranslator<BuilderInstruction21c> {

  public F21cTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction21c i) {
    // Move a reference to the string specified by the given index into the specified register.
    // A: destination register (8 bits)
    // B: string or type index
    int local = i.getRegisterA();
    Reference ref = i.getReference();
    switch (i.getOpcode()) {
    case CONST_STRING:
      il.add(new LdcInsnNode(((StringReference) ref).getString()));
      addLocalSet(local, OBJECT_TYPE);
      return;
    case CONST_CLASS:
      il.add(new LdcInsnNode(Type.getType(((TypeReference) ref).getType())));
      addLocalSet(local, OBJECT_TYPE);
      return;
    case CONST_METHOD_HANDLE:
    case CONST_METHOD_TYPE:
      throw new UnsupportedInsnException(i);
    case CHECK_CAST:
      addLocalGet(local, OBJECT_TYPE);
      il.add(new TypeInsnNode(CHECKCAST, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      return;
    case NEW_INSTANCE:
      il.add(new TypeInsnNode(NEW, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      addLocalSet(local, OBJECT_TYPE);
      return;
    default:
      break;
    }
    FieldReference fr = (FieldReference) ref;
    String owner = Type.getType(fr.getDefiningClass()).getInternalName();
    String name = fr.getName();
    String desc = fr.getType();
    switch (i.getOpcode()) {
    case SGET:
    case SGET_VOLATILE:
    case SGET_BOOLEAN:
    case SGET_BYTE:
    case SGET_CHAR:
    case SGET_SHORT:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSet(local, INT_TYPE);
      break;
    case SGET_WIDE:
    case SGET_WIDE_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSet(local, desc.equals("J") ? LONG_TYPE : DOUBLE_TYPE);
      break;
    case SGET_OBJECT:
    case SGET_OBJECT_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSetObject(local);
      break;
    case SPUT:
    case SPUT_VOLATILE:
    case SPUT_BOOLEAN:
    case SPUT_BYTE:
    case SPUT_CHAR:
    case SPUT_SHORT:
      addLocalGet(local, INT_TYPE);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_WIDE:
    case SPUT_WIDE_VOLATILE:
      addLocalGet(local, desc.equals("J") ? LONG_TYPE : DOUBLE_TYPE);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_OBJECT:
    case SPUT_OBJECT_VOLATILE:
      addLocalGetObject(local);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}