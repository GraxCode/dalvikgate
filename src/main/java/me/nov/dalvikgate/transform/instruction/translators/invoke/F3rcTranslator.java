package me.nov.dalvikgate.transform.instruction.translators.invoke;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.List;

import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc;
import org.jf.dexlib2.iface.reference.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F3rcTranslator extends AbstractInsnTranslator<BuilderInstruction3rc> {

  public F3rcTranslator(InstructionTransformer it) {
    super(it);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void translate(BuilderInstruction3rc i) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C+: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY_RANGE) {
      throw new UnsupportedInsnException("filled-new-array-range", i);
    }
    int registers = i.getRegisterCount(); // sum of all local sizes
    String owner;
    String name;
    String desc;
    List<String> paramTypes;
    if (i.getReferenceType() == ReferenceType.METHOD) {
      MethodReference mr = (MethodReference) i.getReference();
      owner = Type.getType(mr.getDefiningClass()).getInternalName();
      name = mr.getName();
      desc = buildMethodDesc(mr.getParameterTypes(), mr.getReturnType());
      paramTypes = (List<String>) mr.getParameterTypes();
    } else {
      CallSiteReference cs = (CallSiteReference) i.getReference();
      owner = null;
      name = cs.getMethodName();
      paramTypes = (List<String>) cs.getMethodProto().getParameterTypes();
      desc = buildMethodDesc(paramTypes, cs.getMethodProto().getReturnType());
    }
    int parameters = paramTypes.stream().mapToInt(p -> Math.max(1, Type.getType((String) p).getSize())).sum(); // sum of all parameter sizes (parameters + reference = registers)

    boolean hasReference = false;
    if (registers > parameters) {
      if (registers > parameters + 1) {
        throw new IllegalArgumentException("too many registers: " + registers + " for method with desc " + desc);
      }
      addLocalGetObject(i.getStartRegister()); // object reference
      hasReference = true;
    }
    int regIdx = (hasReference ? 1 : 0);
    int parIdx = 0;
    while (regIdx < registers) {
      int register = i.getStartRegister() + regIdx;
      String pDesc = paramTypes.get(parIdx);
      addLocalGet(register, Type.getType(pDesc));
      regIdx += Math.max(1, Type.getType(pDesc).getSize()); // we do not want an infinite loop because of a void argument
      parIdx++;
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
      CallSiteReference cs = (CallSiteReference) i.getReference();
      Object[] args = cs.getExtraArguments().stream().map(v -> DexLibCommons.toObject(v)).toArray();
      il.add(new InvokeDynamicInsnNode(name, desc, DexLibCommons.referenceToASMHandle(cs.getMethodHandle()), args));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}