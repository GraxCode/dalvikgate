package me.nov.dalvikgate.asm;

import java.io.*;
import java.nio.file.Files;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class Conversion {

  /**
   * Default node-to-bytecode conversion. Does not generate frames or compute maxes.
   *
   * @param cn Node to convert.
   * @return Bytecode of class.
   */
  public static byte[] toBytecode(ClassNode cn) {
    ClassWriter cw = new ClassWriter(0);
    cn.accept(cw);
    byte[] b = cw.toByteArray();
    return b;
  }

  /**
   * Configurable node-to-bytecode conversion. Output depends on flags.
   *
   * @param cn    Node to convert.
   * @param flags Writer flags, see {@link ClassWriter#COMPUTE_MAXS} + {@link ClassWriter#COMPUTE_FRAMES}. Using {@code 0} will skip these attributes.
   * @return Bytecode of class.
   */
  public static byte[] toBytecode(ClassNode cn, int flags) {
    ClassWriter cw = new ClassWriter(flags);
    cn.accept(cw);
    byte[] b = cw.toByteArray();
    return b;
  }

  /**
   * Configurable bytecode-to-node conversion. Output depends on flags.
   *
   * @param bytez Bytecode of class.
   * @param flags Reader flags, see {@link ClassReader#SKIP_FRAMES} + {@link ClassReader#SKIP_DEBUG} + {@link ClassReader#SKIP_CODE} for skippable content.<br? See
   *              {@link ClassReader#EXPAND_FRAMES}. <br>
   *              Depending on the use case, certain attributes may want to be skipped or expanded.
   * @return Node of class.
   */
  public static ClassNode toNode(byte[] bytez, int flags) {
    ClassReader cr = new ClassReader(bytez);
    ClassNode cn = new ClassNode();
    cr.accept(cn, flags);
    return cn;
  }

  /**
   * Convert a ClassNode to disassembled text.
   *
   * @param cn Node of class.
   * @return Disassembled code of class.
   */
  public static String textify(ClassNode cn) {
    StringWriter out = new StringWriter();
    new ClassReader(toBytecode(cn)).accept(new TraceClassVisitor(new PrintWriter(out)), ClassReader.SKIP_DEBUG);
    return out.toString();
  }

  /**
   * Write a ClassNode to a file.
   *
   * @param cn Node to save.
   * @throws IOException When writing the file failed.
   */
  public static void saveDebugFile(ClassNode cn) throws IOException {
    Files.write(new File(cn.name.replace('/', '.') + "-debug.class").toPath(), Conversion.toBytecode(cn));
  }
}
