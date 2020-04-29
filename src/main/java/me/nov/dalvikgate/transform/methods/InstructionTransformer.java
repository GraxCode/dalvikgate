package me.nov.dalvikgate.transform.methods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.BuilderOffsetInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction12x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21ih;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21lh;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22b;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22s;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31i;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction51l;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;

@SuppressWarnings("unused")
public class InstructionTransformer implements ITransformer<InsnList>, Opcodes {
  private Map<Integer, Boolean> variableIsObject = new HashMap<>();
  private InsnList il;
  private MethodNode mn;
  private DexBackedMethod method;
  private MutableMethodImplementation builder;
  private HashMap<BuilderInstruction, LabelNode> labels;
  private List<BuilderInstruction> dexInstructions;
  private int argumentRegisterCount;
  private boolean isStatic;

  public InstructionTransformer(MethodNode mn, DexBackedMethod method, MutableMethodImplementation builder) {
    this.mn = mn;
    this.method = method;
    this.builder = builder;
    this.dexInstructions = builder.getInstructions();
    this.isStatic = Access.isStatic(method.accessFlags); // dalvik and java bytecode have the same access values
    // "this" reference is passed as argument in dalvik
    this.argumentRegisterCount = method.getParameters().stream().mapToInt(p -> DexLibCommons.getSize(p)).sum() + (isStatic ? 0 : 1);
  }

  @Override
  public InsnList get() {
    return il;
  }

  private void buildLabels() {
    labels = new HashMap<>();
    // build labels first so we can reference them while rewriting
    for (BuilderInstruction i : dexInstructions) {
      if (!i.getLocation().getLabels().isEmpty()) {
        labels.put(i, new LabelNode());
      }
    }
  }

  private void transformTryCatchBlocks() {
    if (builder.getTryBlocks() != null) {
      mn.tryCatchBlocks = new ArrayList<>();
      builder.getTryBlocks().forEach(tb -> mn.tryCatchBlocks.add(
          new TryCatchBlockNode(getASMLabel(tb.start), getASMLabel(tb.end), getASMLabel(tb.exceptionHandler.getHandler()), Type.getType(tb.exceptionHandler.getExceptionType()).getInternalName())));
    }
  }

  public LabelNode getASMLabel(Label label) {
    return labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst().get());
  }

  private int regToLocal(int register) {
    // The N arguments to a method land in the last N registers of the method's invocation frame, in order
    int startingArgs = builder.getRegisterCount() - argumentRegisterCount;
    if (register >= startingArgs) {
      return register - startingArgs; // 0 is reserved for "this"
    }
    return register + startingArgs;
  }

  @Override
  public void build() {
    il = new InsnList();
    this.buildLabels();
    this.transformTryCatchBlocks();
    for (BuilderInstruction i : dexInstructions) {
      if (labels.containsKey(i)) {
        // add labels to the code
        il.add(labels.get(i));
      }
      switch (i.getFormat()) {
      case ArrayPayload:
        continue;

      ////////////////////////// GOTOS //////////////////////////
      case Format10t:
      case Format20t:
      case Format30t:
        // 8, 16 and 32 bit goto
        il.add(new JumpInsnNode(GOTO, getASMLabel(((BuilderOffsetInstruction) i).getTarget())));
        continue;
      ////////////////////////// VOID RETURNS //////////////////////////
      case Format10x:
        if (i.getOpcode() != Opcode.NOP) {
          // only void returns, ignore NOPs
          il.add(new InsnNode(RETURN));
        }
        continue;
      ////////////////////////// CONST //////////////////////////
      case Format11n:
        // Move the given literal value (sign-extended to 32 bits) into the specified register.
        // A: destination register (4 bits)
        // B: signed int (4 bits)
        BuilderInstruction11n _11n = (BuilderInstruction11n) i;
        il.add(ASMCommons.makeIntPush(_11n.getNarrowLiteral()));
        addVarInsn(ISTORE, regToLocal(_11n.getRegisterA()));
        continue;
      case Format31c:
        // Move a reference to the string specified by the given index into the specified register.
        // A: destination register (8 bits)
        // B: string index
        BuilderInstruction31c _31c = (BuilderInstruction31c) i;
        il.add(new LdcInsnNode(((StringReference) _31c.getReference()).getString()));
        addVarInsn(ISTORE, regToLocal(_31c.getRegisterA()));
        continue;
      case Format31i:
        // 0x14: Move the given literal value into the specified register.
        // A: destination register (8 bits)
        // B: arbitrary 32-bit constant
        // 0x17: Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
        // A: destination register (8 bits)
        // B: signed int (32 bits)
        // TODO unsure if const-wide-32 need some bitshifting
        BuilderInstruction31i _31i = (BuilderInstruction31i) i;
        il.add(ASMCommons.makeIntPush(_31i.getNarrowLiteral()));
        addVarInsn(ISTORE, regToLocal(_31i.getRegisterA()));
        continue;
      case Format51l:
        // Move the given literal value into the specified register-pair.
        // A: destination register (8 bits)
        // B: arbitrary double-width (64-bit) constant
        BuilderInstruction51l _51l = (BuilderInstruction51l) i;
        il.add(new LdcInsnNode(_51l.getWideLiteral()));
        addVarInsn(LSTORE, regToLocal(_51l.getRegisterA()));
        continue;
      case Format11x:
        visitSingleRegister((BuilderInstruction11x) i);
        continue;
      case Format12x:
        visitDoubleRegister((BuilderInstruction12x) i);
        continue;
      case Format21c:
        visitReferenceSingleRegister((BuilderInstruction21c) i);
        continue;
      case Format21ih:
        BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
        il.add(ASMCommons.makeIntPush(_21ih.getNarrowLiteral()));
        addVarInsn(ISTORE, regToLocal(_21ih.getRegisterA()));
        continue;
      case Format21lh:
        // Move the given literal value (right-zero-extended to 64 bits) into the specified register-pair
        // A: destination register (8 bits)
        // B: signed int (16 bits)
        BuilderInstruction21lh _21lh = (BuilderInstruction21lh) i;
        il.add(ASMCommons.makeLongPush(_21lh.getWideLiteral()));
        addVarInsn(LSTORE, regToLocal(_21lh.getRegisterA()));
        continue;
      case Format21s:
        // Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
        // A: destination register (8 bits)
        // B: signed int (16 bits)
        BuilderInstruction21s _21s = (BuilderInstruction21s) i;
        il.add(ASMCommons.makeIntPush(_21s.getNarrowLiteral()));
        addVarInsn(ISTORE, regToLocal(_21s.getRegisterA()));
        continue;
      case Format21t:
        visitSingleJump((BuilderInstruction21t) i);
        continue;
      case Format22b:
        visitInt8Math((BuilderInstruction22b) i);
        continue;
      case Format22c:
        BuilderInstruction22c _22c = (BuilderInstruction22c) i;
        if (_22c.getReferenceType() == ReferenceType.FIELD) {
          FieldReference fieldReference = (FieldReference) _22c.getReference();
          String owner = Type.getType(fieldReference.getDefiningClass()).getInternalName();
          String name = fieldReference.getName();
          String desc = fieldReference.getType();
          addVarInsn(ALOAD, regToLocal(_22c.getRegisterA()));
          il.add(new FieldInsnNode(GETFIELD, owner, name, desc));
          addVarInsn(ASTORE, regToLocal(_22c.getRegisterB()));
          continue;
        } else {
          TypeReference typeReference = (TypeReference) _22c.getReference();
          if (i.getOpcode() == Opcode.INSTANCE_OF) {
            addVarInsn(ALOAD, regToLocal(_22c.getRegisterB()));
            il.add(new TypeInsnNode(INSTANCEOF, Type.getType(typeReference.getType()).getInternalName()));
            addVarInsn(ISTORE, regToLocal(_22c.getRegisterA()));
            continue;
          }
          if (i.getOpcode() == Opcode.NEW_ARRAY) {
            addVarInsn(ILOAD, regToLocal(_22c.getRegisterB()));
            il.add(new TypeInsnNode(ANEWARRAY, Type.getType(typeReference.getType()).getInternalName()));
            addVarInsn(ASTORE, regToLocal(_22c.getRegisterA()));
            continue;
          }
        }
      case Format22cs:
        // iput/iget-type-quick ==> Not listed on dalvik bytecode page?
        throw new IllegalArgumentException("unsupported instruction");
      case Format22s:
        visitInt16Math((BuilderInstruction22s) i);
        continue;
      case Format22t:
        // conditional jumps
        throw new IllegalArgumentException("unsupported instruction");
      case Format22x:
        // move from 16
        throw new IllegalArgumentException("unsupported instruction");
      case Format23x:
        // Perform the indicated floating point or long comparison, setting a to 0 if b == c, 1 if b > c, or -1 if b < c.
        // The "bias" listed for the floating point operations indicates how NaN comparisons are treated: "gt bias" instructions return 1 for NaN comparisons,
        // and "lt bias" instructions return -1.
        //
        // For example, to check to see if floating point x < y it is advisable to use cmpg-float;
        // a result of -1 indicates that the test was true, and the other values indicate it was false
        // either due to a valid comparison or because one of the values was NaN.
        //
        // A: destination register (8 bits)
        // B: first source register or pair
        // C: second source register or pair
        throw new IllegalArgumentException("unsupported instruction");
      case Format31t:
        // fill-array-data and switches
        throw new IllegalArgumentException("unsupported instruction");
      case Format32x:
        // more moves
        throw new IllegalArgumentException("unsupported instruction");
      ////////////////////////// INVOKE //////////////////////////
      case Format35c:
        visitInvoke((BuilderInstruction35c) i);
        continue;
      case Format35ms:
        // invokeQuick
        throw new IllegalArgumentException("unsupported instruction");
      case Format3rc:
        throw new IllegalArgumentException("unsupported instruction");
      ////////////////////////// EXECUTE INLINE //////////////////////////
      case Format35mi:
        // execute inline range
        throw new IllegalArgumentException("unsupported instruction");
      case Format3rmi:
        // execute inline range
        throw new IllegalArgumentException("unsupported instruction");
      case Format3rms:
        // invoke quick range
        throw new IllegalArgumentException("unsupported instruction");
      ////////////////////////// INVOKE POLYMORPHIC //////////////////////////
      case Format45cc:
        throw new IllegalArgumentException("unsupported instruction");
      case Format4rcc:
        throw new IllegalArgumentException("unsupported instruction");
      ////////////////////////// SPECIAL INSTRUCTIONS //////////////////////////
      case Format20bc:
        il.add(ASMCommons.makeExceptionThrow("java/lang/VerifyError", "throw-verification-error instruction"));
        continue;
      case PackedSwitchPayload:
        throw new IllegalArgumentException("unsupported instruction");
      case SparseSwitchPayload:
        throw new IllegalArgumentException("unsupported instruction");
      case UnresolvedOdexInstruction:
      default:
        throw new IllegalArgumentException(i.getClass().getName());
      }
    }
  }

  private void addVarInsn(int opcode, int index) {
    il.add(new VarInsnNode(opcode, index));
    variableIsObject.put(index, (opcode == ALOAD || opcode == ASTORE));
  }

  private void visitInt8Math(BuilderInstruction22b i) {
    // Perform the indicated binary op on the indicated register (first argument) and literal value (second argument),
    // storing the result in the destination register.
    // A: destination register (8 bits)
    // B: source register (8 bits)
    // C: signed int constant (8 bits)
    addVarInsn(ILOAD, regToLocal(i.getRegisterB()));
    addVarInsn(ILOAD, regToLocal(i.getRegisterA()));
    switch (i.getOpcode()) {
    case ADD_INT_LIT8:
      il.add(new InsnNode(IADD));
      break;
    case RSUB_INT_LIT8:
      il.add(new InsnNode(ISUB));
      break;
    case MUL_INT_LIT8:
      il.add(new InsnNode(IMUL));
      break;
    case DIV_INT_LIT8:
      il.add(new InsnNode(IDIV));
      break;
    case REM_INT_LIT8:
      il.add(new InsnNode(IREM));
      break;
    case AND_INT_LIT8:
      il.add(new InsnNode(IAND));
      break;
    case OR_INT_LIT8:
      il.add(new InsnNode(IOR));
      break;
    case XOR_INT_LIT8:
      il.add(new InsnNode(IXOR));
      break;
    case SHL_INT_LIT8:
      il.add(new InsnNode(ISHL));
      break;
    case SHR_INT_LIT8:
      il.add(new InsnNode(ISHR));
      break;
    case USHR_INT_LIT8:
      il.add(new InsnNode(IUSHR));
      break;
    }
    addVarInsn(ISTORE, i.getRegisterA());
  }

  private void visitInt16Math(BuilderInstruction22s i) {
    // Perform the indicated binary op on the indicated register (first argument) and literal value (second argument),
    // storing the result in the destination register.
    // A: destination register (4 bits)
    // B: source register (4 bits)
    // C: signed int constant (16 bits)
    addVarInsn(ILOAD, i.getRegisterB());
    addVarInsn(ILOAD, i.getRegisterA());
    switch (i.getOpcode()) {
    case ADD_INT_LIT8:
      il.add(new InsnNode(IADD));
      break;
    case RSUB_INT_LIT8:
      il.add(new InsnNode(ISUB));
      break;
    case MUL_INT_LIT8:
      il.add(new InsnNode(IMUL));
      break;
    case DIV_INT_LIT8:
      il.add(new InsnNode(IDIV));
      break;
    case REM_INT_LIT8:
      il.add(new InsnNode(IREM));
      break;
    case AND_INT_LIT8:
      il.add(new InsnNode(IAND));
      break;
    case OR_INT_LIT8:
      il.add(new InsnNode(IOR));
      break;
    case XOR_INT_LIT8:
      il.add(new InsnNode(IXOR));
      break;
    case SHL_INT_LIT8:
      il.add(new InsnNode(ISHL));
      break;
    case SHR_INT_LIT8:
      il.add(new InsnNode(ISHR));
      break;
    case USHR_INT_LIT8:
      il.add(new InsnNode(IUSHR));
      break;
    }
    addVarInsn(ISTORE, i.getRegisterA());
  }

  private void visitSingleRegister(BuilderInstruction11x i) {
    // Move immediate word value into register.
    // A: destination register
    int source = regToLocal(i.getRegisterA());
    switch (i.getOpcode()) {
    case MOVE_RESULT:
      addVarInsn(ISTORE, source);
      break;
    case MOVE_EXCEPTION:
    case MOVE_RESULT_OBJECT:
      addVarInsn(ASTORE, source);
      break;
    case MOVE_RESULT_WIDE:
      // TODO move result has method as previous, find out if double or long
      addVarInsn(LSTORE, source);
      break;
    case MONITOR_ENTER:
      addVarInsn(ALOAD, source);
      il.add(new InsnNode(MONITORENTER));
      break;
    case MONITOR_EXIT:
      addVarInsn(ALOAD, source);
      il.add(new InsnNode(MONITOREXIT));
      break;
    case RETURN:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(IRETURN));
      break;
    case RETURN_WIDE:
      // TODO find out if double or long
      addVarInsn(LLOAD, source);
      il.add(new InsnNode(LRETURN));
      break;
    case RETURN_OBJECT:
      addVarInsn(ALOAD, source);
      il.add(new InsnNode(ARETURN));
      break;
    case THROW:
      addVarInsn(ALOAD, source);
      il.add(new InsnNode(ATHROW));
      break;
    default:
      throw new IllegalArgumentException(i.getOpcode().name);
    }
  }

  private void visitDoubleRegister(BuilderInstruction12x i) {
    int source = regToLocal(i.getRegisterB());
    int destination = regToLocal(i.getRegisterA());
    switch (i.getOpcode()) {
    case MOVE:
      if (source == destination)
        break;
      addVarInsn(ILOAD, source);
      addVarInsn(ISTORE, destination);
      break;
    case MOVE_WIDE:
      if (source == destination)
        break;
      // TODO find out if double or long
      addVarInsn(LLOAD, source);
      addVarInsn(LSTORE, destination);
      break;
    case MOVE_OBJECT:
      if (source == destination)
        break;
      addVarInsn(ALOAD, source);
      addVarInsn(ASTORE, destination);
      break;
    case ARRAY_LENGTH:
      addVarInsn(ALOAD, source);
      il.add(new InsnNode(ARRAYLENGTH));
      addVarInsn(ISTORE, destination);
      break;
    case NEG_INT:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(INEG));
      addVarInsn(ISTORE, destination);
      break;
    case NOT_INT:
      throw new IllegalArgumentException("not-int"); // TODO
    case NEG_LONG:
      addVarInsn(LLOAD, source);
      il.add(new InsnNode(LNEG));
      addVarInsn(LSTORE, destination);
      break;
    case NOT_LONG:
      throw new IllegalArgumentException("not-long"); // TODO
    case NEG_FLOAT:
      addVarInsn(FLOAD, source);
      il.add(new InsnNode(FNEG));
      addVarInsn(FSTORE, destination);
      break;
    case NEG_DOUBLE:
      addVarInsn(DLOAD, source);
      il.add(new InsnNode(DNEG));
      addVarInsn(DSTORE, destination);
      break;
    case INT_TO_LONG:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2L));
      addVarInsn(LSTORE, destination);
      break;
    case INT_TO_FLOAT:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2F));
      addVarInsn(FSTORE, destination);
      break;
    case INT_TO_DOUBLE:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2D));
      addVarInsn(DSTORE, destination);
      break;
    case LONG_TO_INT:
      addVarInsn(LLOAD, source);
      il.add(new InsnNode(L2I));
      addVarInsn(ISTORE, destination);
      break;
    case LONG_TO_FLOAT:
      addVarInsn(LLOAD, source);
      il.add(new InsnNode(L2F));
      addVarInsn(FSTORE, destination);
      break;
    case LONG_TO_DOUBLE:
      addVarInsn(LLOAD, source);
      il.add(new InsnNode(L2D));
      addVarInsn(DSTORE, destination);
      break;
    case FLOAT_TO_INT:
      addVarInsn(FLOAD, source);
      il.add(new InsnNode(F2I));
      addVarInsn(ISTORE, destination);
      break;
    case FLOAT_TO_LONG:
      addVarInsn(FLOAD, source);
      il.add(new InsnNode(F2L));
      addVarInsn(LSTORE, destination);
      break;
    case FLOAT_TO_DOUBLE:
      addVarInsn(FLOAD, source);
      il.add(new InsnNode(F2D));
      addVarInsn(DSTORE, destination);
      break;
    case DOUBLE_TO_INT:
      addVarInsn(DLOAD, source);
      il.add(new InsnNode(D2I));
      addVarInsn(ISTORE, destination);
      break;
    case DOUBLE_TO_LONG:
      addVarInsn(DLOAD, source);
      il.add(new InsnNode(D2L));
      addVarInsn(LSTORE, destination);
      break;
    case DOUBLE_TO_FLOAT:
      addVarInsn(DLOAD, source);
      il.add(new InsnNode(D2F));
      addVarInsn(FSTORE, destination);
      break;
    case INT_TO_BYTE:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2B));
      addVarInsn(ISTORE, destination);
      break;
    case INT_TO_CHAR:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2C));
      addVarInsn(ISTORE, destination);
      break;
    case INT_TO_SHORT:
      addVarInsn(ILOAD, source);
      il.add(new InsnNode(I2S));
      addVarInsn(ISTORE, destination);
      break;
    case ADD_INT_2ADDR:
    case SUB_INT_2ADDR:
    case MUL_INT_2ADDR:
    case DIV_INT_2ADDR:
    case REM_INT_2ADDR:
    case AND_INT_2ADDR:
    case OR_INT_2ADDR:
    case XOR_INT_2ADDR:
    case SHL_INT_2ADDR:
    case SHR_INT_2ADDR:
    case USHR_INT_2ADDR:
    case ADD_LONG_2ADDR:
    case SUB_LONG_2ADDR:
    case MUL_LONG_2ADDR:
    case DIV_LONG_2ADDR:
    case REM_LONG_2ADDR:
    case AND_LONG_2ADDR:
    case OR_LONG_2ADDR:
    case XOR_LONG_2ADDR:
    case SHL_LONG_2ADDR:
    case SHR_LONG_2ADDR:
    case USHR_LONG_2ADDR:
    case ADD_FLOAT_2ADDR:
    case SUB_FLOAT_2ADDR:
    case MUL_FLOAT_2ADDR:
    case DIV_FLOAT_2ADDR:
    case REM_FLOAT_2ADDR:
    case ADD_DOUBLE_2ADDR:
    case SUB_DOUBLE_2ADDR:
    case MUL_DOUBLE_2ADDR:
    case DIV_DOUBLE_2ADDR:
    case REM_DOUBLE_2ADDR:
      // TODO destination also contains first source register (first two bits destination, second two source). int source = second source register
      // have to do some bit shifting action here
    default:
      throw new IllegalArgumentException(i.getOpcode().name);
    }
  }

  private void visitReferenceSingleRegister(BuilderInstruction21c i) {
    // Move a reference to the string specified by the given index into the specified register.
    // A: destination register (8 bits)
    // B: string or type index
    int register = regToLocal(i.getRegisterA());
    Reference ref = i.getReference();
    switch (i.getOpcode()) {
    case CONST_STRING:
      il.add(new LdcInsnNode(((StringReference) ref).getString()));
      addVarInsn(ASTORE, register);
      return;
    case CONST_CLASS:
      il.add(new LdcInsnNode(Type.getType(((TypeReference) ref).getType())));
      addVarInsn(ASTORE, register);
      return;
    case CONST_METHOD_HANDLE:
    case CONST_METHOD_TYPE:
      throw new IllegalArgumentException("unsupported instruction: " + i.getOpcode().name);
    case CHECK_CAST:
      addVarInsn(ALOAD, register);
      il.add(new TypeInsnNode(CHECKCAST, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      return;
    case NEW_INSTANCE:
      il.add(new TypeInsnNode(NEW, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      addVarInsn(ASTORE, register);
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
      addVarInsn(ISTORE, register);
      break;
    case SGET_WIDE:
    case SGET_WIDE_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addVarInsn(desc.equals("J") ? LSTORE : DSTORE, register);
      break;
    case SGET_OBJECT:
    case SGET_OBJECT_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addVarInsn(ASTORE, register);
      break;
    case SPUT:
    case SPUT_VOLATILE:
    case SPUT_BOOLEAN:
    case SPUT_BYTE:
    case SPUT_CHAR:
    case SPUT_SHORT:
      addVarInsn(ILOAD, register);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_WIDE:
    case SPUT_WIDE_VOLATILE:
      addVarInsn(desc.equals("J") ? LLOAD : DLOAD, register);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_OBJECT:
    case SPUT_OBJECT_VOLATILE:
      addVarInsn(ALOAD, register);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    default:
      throw new IllegalArgumentException(i.getOpcode().name);
    }
  }

  private void visitSingleJump(BuilderInstruction21t i) {
    // Branch to the given destination if the given register's value compares with 0 as specified.
    // A: register to test (8 bits)
    // B: signed branch offset (16 bits)
    int source = regToLocal(i.getRegisterA());
    // TODO check if object
    // Dalvik has no ifnull / ifnonnull
    // So we must track variable usage and infer the type. Is it an object or primitive?
    boolean refIsObject = variableIsObject.getOrDefault(source, false);
    Label label = i.getTarget();
    il.add(new VarInsnNode(refIsObject ? ALOAD : ILOAD /* get type here */, source));
    switch (i.getOpcode()) {
    case IF_EQZ:
      il.add(new JumpInsnNode(refIsObject ? IFNULL : IFEQ, getASMLabel(label)));
      break;
    case IF_NEZ:
      il.add(new JumpInsnNode(refIsObject ? IFNONNULL : IFNE, getASMLabel(label)));
      break;
    case IF_LTZ:
      il.add(new JumpInsnNode(IFLT, getASMLabel(label)));
      break;
    case IF_GEZ:
      il.add(new JumpInsnNode(IFGE, getASMLabel(label)));
      break;
    case IF_GTZ:
      il.add(new JumpInsnNode(IFGT, getASMLabel(label)));
      break;
    case IF_LEZ:
      il.add(new JumpInsnNode(IFLE, getASMLabel(label)));
      break;
    default:
      throw new IllegalArgumentException(i.getOpcode().name);
    }
  }

  /**
   * Visit invoke instruction with a maximum of 5 arguments
   *
   * @param i
   */
  private void visitInvoke(BuilderInstruction35c i) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C..G: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY) {
      throw new IllegalArgumentException("filled-new-array");
    }
    MethodReference mr = (MethodReference) i.getReference();
    String owner = Type.getType(mr.getDefiningClass()).getInternalName();
    String name = mr.getName();
    String desc = ASMCommons.buildMethodDesc(mr.getParameterTypes(), mr.getReturnType());
    int registers = i.getRegisterCount(); // sum of all local sizes
    int parameters = mr.getParameterTypes().stream().mapToInt(p -> Type.getType((String) p).getSize()).sum(); // sum of all parameter sizes (parameters + reference = registers)
    int parIdx = 0;
    int regIdx = 0;
    if (registers > parameters) {
      if (registers > parameters + 1) {
        throw new IllegalArgumentException("too many registers: " + registers + " for method with desc " + desc);
      }
      addVarInsn(ALOAD, regToLocal(i.getRegisterC()));
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
        throw new IllegalArgumentException("more than 5 parameters: " + desc);
      }
      regIdx++;
      String pDesc = parameterTypes.get(parIdx);
      addVarInsn(ASMCommons.getLoadOpForDesc(pDesc), regToLocal(register));
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
      throw new IllegalArgumentException(i.getOpcode().name);
    }
  }
}
