package me.nov.dalvikgate.transform.methods;

import java.util.HashMap;
import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction12x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction20t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21ih;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction30t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31i;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.reference.FieldReference;
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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;

/**
 * FIXME two-word variables are not identifiable in dalvik, find out if either long or double. shouldn't be that much of a problem
 */
@SuppressWarnings("unused")
public class InstructionTransformer implements ITransformer<InsnList>, Opcodes {

	private InsnList il;
	private MethodNode mn;
	private DexBackedMethod method;
	private MutableMethodImplementation builder;
	private HashMap<BuilderInstruction, LabelNode> labels;
	private List<BuilderInstruction> dexInstructions;

	public InstructionTransformer(MethodNode mn, DexBackedMethod method, MutableMethodImplementation builder) {
		this.mn = mn;
		this.method = method;
		this.builder = builder;
		this.dexInstructions = builder.getInstructions();
	}

	private void buildLabels() {
		labels = new HashMap<>();
		// build labels first so we can reference them while rewriting;
		for (BuilderInstruction i : dexInstructions) {
			if (!i.getLocation().getLabels().isEmpty()) {
				labels.put(i, new LabelNode());
			}
		}
	}

	public LabelNode getASMLabel(Label label) {
		return labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst().get());
	}

	@Override
	public InsnList get() {
		return il;
	}

	@Override
	public void build() {
		il = new InsnList();
		this.buildLabels();

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
				// 8 bit goto
				il.add(new JumpInsnNode(GOTO, getASMLabel(((BuilderInstruction10t) i).getTarget())));
				continue;
			case Format20t:
				// 16 bit goto
				il.add(new JumpInsnNode(GOTO, getASMLabel(((BuilderInstruction20t) i).getTarget())));
				continue;
			case Format30t:
				// 32 bit goto
				il.add(new JumpInsnNode(GOTO, getASMLabel(((BuilderInstruction30t) i).getTarget())));
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
				// const 4 bits
				BuilderInstruction11n _11n = (BuilderInstruction11n) i;
				il.add(ASMCommons.makeIntPush(_11n.getNarrowLiteral()));
				il.add(new VarInsnNode(ISTORE, _11n.getRegisterA()));
				continue;
			case Format31c:
				// const jumbo string
				BuilderInstruction31c _31c = (BuilderInstruction31c) i;
				il.add(new LdcInsnNode(((StringReference) _31c.getReference()).getString()));
				il.add(new VarInsnNode(ISTORE, _31c.getRegisterA()));
				continue;
			case Format31i:
				// const 32 bits
				// TODO unsure if const-wide-32 need some bitshifting
				BuilderInstruction31i _31i = (BuilderInstruction31i) i;
				il.add(ASMCommons.makeIntPush(_31i.getNarrowLiteral()));
				il.add(new VarInsnNode(ISTORE, _31i.getRegisterA()));
				continue;

			case Format11x:
				visitSingleRegister((BuilderInstruction11x) i);
				continue;
			case Format12x:
				visitDoubleRegister((BuilderInstruction12x) i);
				continue;
			case Format20bc:
				il.add(ASMCommons.makeExceptionThrow("java/lang/VerifyError", "throw-verification-error instruction"));
				continue;
			case Format21c:
				visitReferenceSingleRegister((BuilderInstruction21c) i);
				continue;
			case Format21ih:
				BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
				il.add(ASMCommons.makeIntPush(_21ih.getNarrowLiteral()));
				il.add(new VarInsnNode(ISTORE, _21ih.getRegisterA()));
				continue;
			case Format21lh: // TODO
				throw new IllegalArgumentException("unsupported instruction: const-wide-high16");
			case Format21s:
				BuilderInstruction21s _21s = (BuilderInstruction21s) i;
				if (i.getOpcode() == Opcode.CONST_WIDE_16) {
					// TODO
					throw new IllegalArgumentException("unsupported instruction: const-wide-16 with value " + _21s.getNarrowLiteral());
				}
				il.add(ASMCommons.makeIntPush(_21s.getNarrowLiteral()));
				il.add(new VarInsnNode(ISTORE, _21s.getRegisterA()));
				continue;
			case Format21t:
				visitIntJump((BuilderInstruction21t) i);
				continue;
			case Format22b:
				throw new IllegalArgumentException("unsupported instruction");
			case Format22c:
				throw new IllegalArgumentException("unsupported instruction");
			case Format22cs:
				throw new IllegalArgumentException("unsupported instruction");
			case Format22s:
				throw new IllegalArgumentException("unsupported instruction");
			case Format22t:
				// conditional jumps
				throw new IllegalArgumentException("unsupported instruction");
			case Format22x:
				// move from 16
				throw new IllegalArgumentException("unsupported instruction");
			case Format23x:
				throw new IllegalArgumentException("unsupported instruction");
			case Format31t:
				// fill-array-data and switches
				throw new IllegalArgumentException("unsupported instruction");
			case Format32x:
				// more moves
				throw new IllegalArgumentException("unsupported instruction");

			case Format35c:
				visit35c((BuilderInstruction35c) i);
				continue;
			case Format35mi:
				throw new IllegalArgumentException("unsupported instruction");
			case Format35ms:
				throw new IllegalArgumentException("unsupported instruction");
			case Format3rc:
				throw new IllegalArgumentException("unsupported instruction");
			case Format3rmi:
				throw new IllegalArgumentException("unsupported instruction");
			case Format3rms:
				throw new IllegalArgumentException("unsupported instruction");
			case Format45cc:
				throw new IllegalArgumentException("unsupported instruction");
			case Format4rcc:
				throw new IllegalArgumentException("unsupported instruction");
			case Format51l:
				throw new IllegalArgumentException("unsupported instruction");
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

	private void visitSingleRegister(BuilderInstruction11x i) {
		int source = i.getRegisterA();
		switch (i.getOpcode()) {
		case MOVE_RESULT:
			il.add(new VarInsnNode(ISTORE, source));
			break;
		case MOVE_EXCEPTION:
		case MOVE_RESULT_OBJECT:
			il.add(new VarInsnNode(ASTORE, source));
			break;
		case MOVE_RESULT_WIDE:
			// TODO move result has method as previous, find out if double or long
			il.add(new VarInsnNode(LSTORE, source));
			break;
		case MONITOR_ENTER:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new InsnNode(MONITORENTER));
			break;
		case MONITOR_EXIT:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new InsnNode(MONITOREXIT));
			break;
		case RETURN:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(IRETURN));
			break;
		case RETURN_WIDE:
			// TODO find out if double or long
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new InsnNode(LRETURN));
			break;
		case RETURN_OBJECT:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new InsnNode(ARETURN));
			break;
		case THROW:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new InsnNode(ATHROW));
			break;
		default:
			throw new IllegalArgumentException(i.getOpcode().name);
		}
	}

	private void visitDoubleRegister(BuilderInstruction12x i) {
		int source = i.getRegisterB();
		int destination = i.getRegisterA();
		switch (i.getOpcode()) {
		case MOVE:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case MOVE_WIDE:
			// TODO find out if double or long
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new VarInsnNode(LSTORE, destination));
			break;
		case MOVE_OBJECT:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new VarInsnNode(ASTORE, destination));
			break;
		case ARRAY_LENGTH:
			il.add(new VarInsnNode(ALOAD, source));
			il.add(new InsnNode(ARRAYLENGTH));
			il.add(new VarInsnNode(ASTORE, destination));
			break;
		case NEG_INT:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(INEG));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case NOT_INT:
			throw new IllegalArgumentException("not-int"); // TODO
		case NEG_LONG:
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new InsnNode(LNEG));
			il.add(new VarInsnNode(LSTORE, destination));
			break;
		case NOT_LONG:
			throw new IllegalArgumentException("not-long"); // TODO
		case NEG_FLOAT:
			il.add(new VarInsnNode(FLOAD, source));
			il.add(new InsnNode(FNEG));
			il.add(new VarInsnNode(FSTORE, destination));
			break;
		case NEG_DOUBLE:
			il.add(new VarInsnNode(DLOAD, source));
			il.add(new InsnNode(DNEG));
			il.add(new VarInsnNode(DSTORE, destination));
			break;
		case INT_TO_LONG:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2L));
			il.add(new VarInsnNode(LSTORE, destination));
			break;
		case INT_TO_FLOAT:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2F));
			il.add(new VarInsnNode(FSTORE, destination));
			break;
		case INT_TO_DOUBLE:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2D));
			il.add(new VarInsnNode(DSTORE, destination));
			break;
		case LONG_TO_INT:
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new InsnNode(L2I));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case LONG_TO_FLOAT:
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new InsnNode(L2F));
			il.add(new VarInsnNode(FSTORE, destination));
			break;
		case LONG_TO_DOUBLE:
			il.add(new VarInsnNode(LLOAD, source));
			il.add(new InsnNode(L2D));
			il.add(new VarInsnNode(DSTORE, destination));
			break;
		case FLOAT_TO_INT:
			il.add(new VarInsnNode(FLOAD, source));
			il.add(new InsnNode(F2I));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case FLOAT_TO_LONG:
			il.add(new VarInsnNode(FLOAD, source));
			il.add(new InsnNode(F2L));
			il.add(new VarInsnNode(LSTORE, destination));
			break;
		case FLOAT_TO_DOUBLE:
			il.add(new VarInsnNode(FLOAD, source));
			il.add(new InsnNode(F2D));
			il.add(new VarInsnNode(DSTORE, destination));
			break;
		case DOUBLE_TO_INT:
			il.add(new VarInsnNode(DLOAD, source));
			il.add(new InsnNode(D2I));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case DOUBLE_TO_LONG:
			il.add(new VarInsnNode(DLOAD, source));
			il.add(new InsnNode(D2L));
			il.add(new VarInsnNode(LSTORE, destination));
			break;
		case DOUBLE_TO_FLOAT:
			il.add(new VarInsnNode(DLOAD, source));
			il.add(new InsnNode(D2F));
			il.add(new VarInsnNode(FSTORE, destination));
			break;
		case INT_TO_BYTE:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2B));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case INT_TO_CHAR:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2C));
			il.add(new VarInsnNode(ISTORE, destination));
			break;
		case INT_TO_SHORT:
			il.add(new VarInsnNode(ILOAD, source));
			il.add(new InsnNode(I2S));
			il.add(new VarInsnNode(ISTORE, destination));
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
		int register = i.getRegisterA();
		Reference ref = i.getReference();
		switch (i.getOpcode()) {
		case CONST_STRING:
			il.add(new LdcInsnNode(((StringReference) ref).getString()));
			il.add(new VarInsnNode(ASTORE, register));
			return;
		case CONST_CLASS:
			il.add(new LdcInsnNode(Type.getType(((TypeReference) ref).getType())));
			il.add(new VarInsnNode(ASTORE, register));
			return;
		case CONST_METHOD_HANDLE:
		case CONST_METHOD_TYPE:
			throw new IllegalArgumentException("unsupported instruction: " + i.getOpcode().name);
		case CHECK_CAST:
			il.add(new VarInsnNode(ALOAD, register));
			il.add(new TypeInsnNode(CHECKCAST, Type.getType(((TypeReference) ref).getType()).getInternalName()));
			return;
		case NEW_INSTANCE:
			il.add(new TypeInsnNode(NEW, Type.getType(((TypeReference) ref).getType()).getInternalName()));
			il.add(new VarInsnNode(ASTORE, register));
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
			il.add(new VarInsnNode(ISTORE, register));
			break;
		case SGET_WIDE:
		case SGET_WIDE_VOLATILE:
			il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
			il.add(new VarInsnNode(desc.equals("J") ? LSTORE : DSTORE, register));
			break;
		case SGET_OBJECT:
		case SGET_OBJECT_VOLATILE:
			il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
			il.add(new VarInsnNode(ASTORE, register));
			break;
		case SPUT:
		case SPUT_VOLATILE:
		case SPUT_BOOLEAN:
		case SPUT_BYTE:
		case SPUT_CHAR:
		case SPUT_SHORT:
			il.add(new VarInsnNode(ILOAD, register));
			il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
			break;
		case SPUT_WIDE:
		case SPUT_WIDE_VOLATILE:
			il.add(new VarInsnNode(desc.equals("J") ? LLOAD : DLOAD, register));
			il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
			break;
		case SPUT_OBJECT:
		case SPUT_OBJECT_VOLATILE:
			il.add(new VarInsnNode(ALOAD, register));
			il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
			break;
		default:
			throw new IllegalArgumentException(i.getOpcode().name);
		}
	}

	private void visitIntJump(BuilderInstruction21t i) {
		int source = i.getRegisterA();
		Label label = i.getTarget();
		il.add(new VarInsnNode(ILOAD, source));
		switch (i.getOpcode()) {
		case IF_EQZ:
			il.add(new JumpInsnNode(IFEQ, getASMLabel(label)));
			break;
		case IF_NEZ:
			il.add(new JumpInsnNode(IFNE, getASMLabel(label)));
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

	private void visit35c(BuilderInstruction35c i) { // TODO
		switch (i.getOpcode()) {
		default:
			break;
//		 INVOKE_VIRTUAL
//	    INVOKE_SUPER
//	    INVOKE_DIRECT
//	    INVOKE_STATIC
//	    INVOKE_INTERFACE
//	    INVOKE_VIRTUAL_RANGE
//	    INVOKE_SUPER_RANGE
//	    INVOKE_DIRECT_RANGE
//	    INVOKE_STATIC_RANGE
//	    INVOKE_INTERFACE_RANGE
		// INVOKE_CUSTOM
		// FILLED_NEW_ARRAY
		}
	}

}
