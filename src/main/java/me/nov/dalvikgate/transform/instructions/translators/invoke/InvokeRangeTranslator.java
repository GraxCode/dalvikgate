package me.nov.dalvikgate.transform.instructions.translators.invoke;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.List;

import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc;
import org.jf.dexlib2.iface.reference.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;

public class InvokeRangeTranslator extends AbstractInsnTranslator<BuilderInstruction3rc> {

  public InvokeRangeTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction3rc i) {
    this.translate(i, getNextOf(i));
  }

  /**
   * Special method for instructions that are not in the code
   */
  @SuppressWarnings("unchecked")
  public void translate(BuilderInstruction3rc i, BuilderInstruction next) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C+: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY_RANGE) {
      translateFilledNewArrayRange(i, next);
      return;
    }
    String owner;
    String name;
    String desc;
    List<String> paramTypes;
    String returnType;
    if (i.getReferenceType() == ReferenceType.METHOD) {
      MethodReference mr = (MethodReference) i.getReference();
      owner = Type.getType(mr.getDefiningClass()).getInternalName();
      name = mr.getName();
      paramTypes = (List<String>) mr.getParameterTypes();
      returnType = mr.getReturnType();
      desc = buildMethodDesc(paramTypes, returnType);
    } else {
      CallSiteReference cs = (CallSiteReference) i.getReference();
      owner = null;
      name = cs.getMethodName();
      paramTypes = (List<String>) cs.getMethodProto().getParameterTypes();
      returnType = cs.getMethodProto().getReturnType();
      desc = buildMethodDesc(paramTypes, returnType);
    }

    int registers = i.getRegisterCount(); // sum of all local sizes
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
    int returnSize = Type.getType(returnType).getSize();
    if (returnSize > 0) {
      switch (next.getOpcode()) {
      case MOVE_RESULT:
      case MOVE_RESULT_OBJECT:
      case MOVE_RESULT_WIDE:
        // result is stored
        return;
      default:
        // result is lost, pop off stack
        il.add(new InsnNode(returnSize > 1 ? POP2 : POP));
        return;
      }
    }
  }

  private void translateFilledNewArrayRange(BuilderInstruction3rc i, BuilderInstruction next) {
    TypeReference tr = (TypeReference) i.getReference();
    Type elementType = Type.getType(tr.getType()).getElementType(); // only non-wide elementType allowed for filled-new-array
    int registers = i.getRegisterCount();
    int regIdx = 0;

    il.add(makeIntPush(registers)); // array size
    if (elementType.getSort() == Type.OBJECT) {
      il.add(new TypeInsnNode(ANEWARRAY, elementType.getInternalName()));
    } else {
      il.add(new IntInsnNode(NEWARRAY, getPrimitiveIndex(elementType.getDescriptor())));
    }
    while (regIdx < registers) {
      int register = i.getStartRegister() + regIdx;
      il.add(new InsnNode(DUP)); // dup array and leave it on stack after loop
      il.add(makeIntPush(regIdx)); // array index
      addLocalGet(register, elementType);
      il.add(new InsnNode(elementType.getOpcode(IASTORE))); // get store instruction for type
      regIdx++; // register can only be 1
    }
    if (next.getOpcode() != Opcode.MOVE_RESULT_OBJECT) {
      il.add(new InsnNode(POP)); // array isn't used, pop
    }
  }
}