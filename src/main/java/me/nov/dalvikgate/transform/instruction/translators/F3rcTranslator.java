package me.nov.dalvikgate.transform.instruction.translators;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F3rcTranslator extends AbstractInsnTranslator<BuilderInstruction3rc> {

  public F3rcTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction3rc i) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C+: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY_RANGE) {
      throw new UnsupportedInsnException("filled-new-array-range", i);
    }
    MethodReference mr = (MethodReference) i.getReference();
    String owner = Type.getType(mr.getDefiningClass()).getInternalName();
    String name = mr.getName();
    String desc = buildMethodDesc(mr.getParameterTypes(), mr.getReturnType());
    int registers = i.getRegisterCount(); // sum of all local sizes
    int parameters = mr.getParameterTypes().stream().mapToInt(p -> Type.getType((String) p).getSize()).sum(); // sum of all parameter sizes (parameters + reference = registers)
    boolean hasReference = false;
    if (registers > parameters) {
      if (registers > parameters + 1) {
        throw new IllegalArgumentException("too many registers: " + registers + " for method with desc " + desc);
      }
      addLocalGetObject(i.getStartRegister()); // object reference
      hasReference = true;
    }
    @SuppressWarnings("unchecked")
    List<String> parameterTypes = (List<String>) mr.getParameterTypes();
    for (int j = 0; j < parameters; j++) {
      int register = i.getStartRegister() + j + (hasReference ? 1 : 0);

      String pDesc = parameterTypes.get(j);
      addLocalGet(register, Type.getType(pDesc));
    }
    switch (i.getOpcode()) {
    case INVOKE_SUPER_RANGE:
      il.add(new MethodInsnNode(INVOKESPECIAL, owner, name, desc));
      break;
    case INVOKE_VIRTUAL_RANGE:
      il.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_DIRECT_RANGE:
    case INVOKE_OBJECT_INIT_RANGE: // TODO unsure what this does
      il.add(new MethodInsnNode(name.equals("<init>") ? INVOKESPECIAL : INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_STATIC_RANGE:
      il.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc));
      break;
    case INVOKE_INTERFACE_RANGE:
      il.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc));
      break;
    case INVOKE_CUSTOM_RANGE:
      // like invokedynamic
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}