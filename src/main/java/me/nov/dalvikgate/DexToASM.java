package me.nov.dalvikgate;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.transform.classes.ClassTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnresolvedInsnException;

public class DexToASM {
  public static void main(String[] args) throws IOException {
    if (args.length == 2) {
      saveAsJar(new File(args[1]), convertToASMTree(new File(args[0])));
    } else {
      System.out.println("java -jar dalvikgate.jar <input.dex> <output.jar>");
    }
  }

  private static List<ClassNode> convertToASMTree(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(52));
    Set<? extends DexBackedClassDef> baseClassDefs = baseBackedDexFile.getClasses();
    List<ClassNode> asmClasses = new ArrayList<>();

    for (DexBackedClassDef clazz : baseClassDefs) {
      ClassTransformer ct = new ClassTransformer();
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
        try {
          out.putNextEntry(new JarEntry(c.name + ".class"));
          out.write(Conversion.toBytecode(c));
          out.closeEntry();
        } catch (UnresolvedInsnException e) {
          System.err.println("SKIP: " + c.name + " - " + e.getMessage());
        } catch (Exception e) {
          // TODO: Better Log and alert user
          System.err.println("FAIL: " + c.name);
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
}
