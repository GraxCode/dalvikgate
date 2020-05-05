package me.nov.dalvikgate.transform.instructions.translators.jump;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.List;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.TranslationException;


/**
 * Payload instructions
 */
public class PayloadTranslator extends AbstractInsnTranslator<BuilderInstruction31t> {

  public PayloadTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction31t i) {
    int register = i.getRegisterA();
    Instruction payload = i.getTarget().getLocation().getInstruction();
    if (i.getOpcode() == Opcode.FILL_ARRAY_DATA) {
      // no floating point or boolean arrays allowed
      BuilderArrayPayload arrayPayload = (BuilderArrayPayload) payload;
      int width = arrayPayload.getElementWidth();
      List<Number> elements = arrayPayload.getArrayElements();

      addLocalGetObject(register); // fill array data takes array object

      for (int f = 0; f < elements.size(); f++) {
        if (f < elements.size() - 1) { // use remaining array on stack and don't dup
          il.add(new InsnNode(DUP)); // dup array
        }
        il.add(makeIntPush(f)); // array index
        il.add(width > 4 ? makeLongPush(elements.get(f).longValue()) : makeIntPush(elements.get(f).intValue()));
        il.add(arrayStoreInstruction(width));
      }
      return;
    }
    addLocalGet(register, Type.INT_TYPE); // switch takes int type
    boolean needsNewDfltLabel = needsNewDefaultLabel(i);
    LabelNode dflt = needsNewDfltLabel ? new LabelNode() : getNextLabel(i);
    if (i.getOpcode() == Opcode.PACKED_SWITCH) {
      BuilderPackedSwitchPayload packedPayload = (BuilderPackedSwitchPayload) payload;
      List<BuilderSwitchElement> pse = packedPayload.getSwitchElements();
      il.add(new TableSwitchInsnNode(pse.get(0).getKey(), pse.get(pse.size() - 1).getKey(), dflt, pse.stream().map(caze -> toASMLabel(caze.getTarget())).toArray(LabelNode[]::new)));
    } else {
      BuilderSparseSwitchPayload sparsePayload = (BuilderSparseSwitchPayload) payload;
      List<BuilderSwitchElement> sse = sparsePayload.getSwitchElements();
      il.add(new LookupSwitchInsnNode(dflt, sse.stream().map(caze -> caze.getKey()).mapToInt(key -> key).toArray(), sse.stream().map(caze -> toASMLabel(caze.getTarget())).toArray(LabelNode[]::new)));
    }

    if (needsNewDfltLabel) {
      il.add(dflt); // add newly created label for default case
    }
  }

  private boolean needsNewDefaultLabel(BuilderInstruction31t i) {
    return getNextOf(i).getLocation().getLabels().isEmpty();
  }

  private LabelNode getNextLabel(BuilderInstruction31t i) {
    return toASMLabel(getNextOf(i).getLocation().getLabels().iterator().next());
  }

  private AbstractInsnNode arrayStoreInstruction(int elementWidth) {
    switch (elementWidth) {
    case 1:
      return new InsnNode(BASTORE);
    case 2:
      return new InsnNode(SASTORE);
    case 4:
      return new InsnNode(IASTORE);
    case 8:
      return new InsnNode(LASTORE);
    }
    throw new TranslationException("size " + elementWidth);
  }
}
