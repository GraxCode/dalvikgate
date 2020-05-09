package me.nov.dalvikgate;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import me.nov.dalvikgate.graph.Inheritance;
import me.nov.dalvikgate.utils.LogWrapper;
import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.transform.classes.ClassTransformer;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;


public class DexToASM {
  public static final LogWrapper logger = new LogWrapper();
  public static final Inheritance rootInheritGraph = new Inheritance();
  public static boolean noResolve;
  public static boolean noOptimize;
  public static String nameFilter;


  public static void dex2Jar(File inDex, File outJar) throws IOException {
    saveAsJar(outJar, convertToASMTree(inDex));
  }

  public static List<ClassNode> convertToASMTree(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(52));
    return convertToASMTree(baseBackedDexFile);
  }

  public static List<ClassNode> convertToASMTree(DexBackedDexFile baseBackedDexFile) throws IOException {
    // Create new inheritance tree that includes files in the dex we want to convert
    Inheritance inheritance = rootInheritGraph.copy();
    inheritance.addDex(baseBackedDexFile);
    // Conversion process
    List<ClassNode> asmClasses = new ArrayList<>();
    for (DexBackedClassDef clazz : baseBackedDexFile.getClasses()) {
      ClassTransformer ct = new ClassTransformer(inheritance);
      ct.visit(clazz);
      asmClasses.add(ct.getTransformed());
    }
    return asmClasses;
  }

  public static void saveAsJar(File output, List<ClassNode> classes) {
    try {
      JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
      out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      out.write(makeManifest().getBytes());
      out.closeEntry();
      for (ClassNode c : classes) {
        if (!c.name.matches(nameFilter))
          continue;
        try {
          out.putNextEntry(new JarEntry(c.name + ".class"));
          out.write(Conversion.toBytecode(c));
          out.closeEntry();
        } catch (UnresolvedInsnException e) {
          DexToASM.logger.error("SKIP: {} - Reason: {}", c.name, e.getMessage());
        } catch (Exception e) {
          DexToASM.logger.error("FAIL: {} - Reason: {}", c.name, e.getMessage());
          e.printStackTrace();
        }
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String makeManifest() {
    String lineSeparator = "\r\n";
    StringBuilder manifest = new StringBuilder();
    manifest.append("Manifest-Version: 1.0");
    manifest.append(lineSeparator);
    manifest.append("Transformed-By: dalvikgate ");
    manifest.append(getVersion());
    manifest.append(lineSeparator);
    return manifest.toString();
  }

  public static String getVersion() {
    try {
      return Objects.requireNonNull(DexToASM.class.getPackage().getImplementationVersion());
    } catch (NullPointerException e) {
      return "(dev)";
    }
  }

  static {
    try {
      long start = System.currentTimeMillis();
      File andoidLib = new File("lib/android.jar");
      DexToASM.logger.info("Populating root inheritance tree...");
      rootInheritGraph.addClasspath();
      if (andoidLib.exists()) {
        rootInheritGraph.addDirectory(andoidLib);
      }
      DexToASM.logger.info("Finished root inheritance, took {}ms", (System.currentTimeMillis() - start));
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to setup initial inheritance graph");
    }
  }
}
