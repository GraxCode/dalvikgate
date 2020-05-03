package me.nov.dalvikgate.transform.instruction.translators.invoke;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F35cTranslator extends AbstractInsnTranslator<BuilderInstruction35c> {

  public F35cTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction35c i) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C..G: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY) {
      throw new UnsupportedInsnException("filled-new-array", i);
    }
    MethodReference mr = (MethodReference) i.getReference();
    String owner = Type.getType(mr.getDefiningClass()).getInternalName();
    String name = mr.getName();
    String desc = buildMethodDesc(mr.getParameterTypes(), mr.getReturnType());
    int registers = i.getRegisterCount(); // sum of all local sizes
    int parameters = mr.getParameterTypes().stream().mapToInt(p -> Type.getType((String) p).getSize()).sum(); // sum of all parameter sizes (parameters + reference = registers)
    int parIdx = 0;
    int regIdx = 0;
    if (registers > parameters) {
      if (registers > parameters + 1) {
        throw new IllegalArgumentException("too many registers: " + registers + " for method with desc " + desc);
      }
      // TODO: Check parameter type?
      // - then use addLocalGet(register, <<TYPE>>) instead of null
      addLocalGetObject(i.getRegisterC());
      registers--; // reference can only be size 1
      regIdx++;
    }
    @SuppressWarnings("unchecked")
    List<String> parameterTypes = (List<String>) mr.getParameterTypes();
    while (registers > 0) {
      int register;
      switch (regIdx) {
      case 0:
        register = i.getRegisterC();
        break;
      case 1:
        register = i.getRegisterD();
        break;
      case 2:
        register = i.getRegisterE();
        break;
      case 3:
        register = i.getRegisterF();
        break;
      case 4:
        register = i.getRegisterG();
        break;
      default:
        throw new IllegalArgumentException("more than 5 registers: " + desc);
      }
      regIdx++;
      String pDesc = parameterTypes.get(parIdx);
      addLocalGet(register, Type.getType(pDesc));
      registers -= Type.getType(pDesc).getSize();
      parIdx++;
    }
    switch (i.getOpcode()) {
    case INVOKE_SUPER:
    case INVOKE_DIRECT_EMPTY:
      il.add(new MethodInsnNode(INVOKESPECIAL, owner, name, desc));
      break;
    case INVOKE_VIRTUAL:
      il.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_DIRECT:
      il.add(new MethodInsnNode(name.equals("<init>") ? INVOKESPECIAL : INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_STATIC:
      il.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc));
      break;
    case INVOKE_INTERFACE:
      il.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc));

      break;
    case INVOKE_CUSTOM:
      // like invokedynamic
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}