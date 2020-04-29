package me.nov.dalvikgate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.transform.classes.ClassTransformer;

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
      ClassTransformer transformer = new ClassTransformer(clazz, 52);
      transformer.build();
      asmClasses.add(transformer.getTransformed());
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
        } catch (Exception e) {
          // TODO: Log and alert user
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
