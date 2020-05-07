package me.nov.dalvikgate.graph;

import me.coley.analysis.value.VirtualValue;
import me.nov.dalvikgate.utils.SetMap;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Simple class inheritance graph.
 */
public class Inheritance {
  private static final String MAP_KV_SPLIT = ":::";
  private static final String MAP_VAL_SPLIT = ",";
  private SetMap<String, String> parentsOf = new SetMap<>();
  private SetMap<String, String> childrenOf = new SetMap<>();
  private SetMap<String, String> parentsOfCachedAll = new SetMap<>();
  private SetMap<String, String> childrenOfCachedAll = new SetMap<>();

  /**
   * Add classes from the current classpath to the inheritance graph.
   *
   * @throws IOException When a classpath item cannot be added.
   */
  public void addClasspath() throws IOException {
    String path = System.getProperty("java.class.path");
    String separator = System.getProperty("path.separator");
    String localDir = System.getProperty("user.dir");
    if (path != null && !path.isEmpty()) {
      String[] items = path.split(separator);
      for (String item : items) {
        Path filePath = Paths.get(item);
        boolean isAbsolute = filePath.isAbsolute();
        File file;
        if (isAbsolute)
          file = new File(item);
        else
          file = Paths.get(localDir, item).toFile();
        if (!file.exists())
          continue;
        if (file.isDirectory())
          addDirectory(file);
        else if (file.getName().endsWith(".jar"))
          addJar(file);
      }
    }
  }

  /**
   * Add classes from the given directory to the inheritance graph.
   *
   * @param dir Directory to use.
   * @throws IOException When walking the directory fails
   */
  public void addDirectory(File dir) throws IOException {
    Files.walkFileTree(Paths.get(dir.getAbsolutePath()), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".class"))
          addClass(file.toFile());
        else if (file.toString().endsWith(".jar"))
          addJar(file.toFile());
        else if (file.toString().endsWith(".dgz"))
          addDalikGateMap(file.toFile());
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Add classes from the given map file to the inheritance graph.
   *
   * @param dgz Dalvikgate map file to use.
   * @throws IOException When reading classes from the jar fails.
   */
  private void addDalikGateMap(File dgz) throws IOException {
    ZipFile zipArchive = new ZipFile(dgz);
    try (ZipInputStream jis = new ZipInputStream(new FileInputStream(dgz))) {
      ZipEntry e;
      int nRead;
      byte[] data = new byte[1024];
      while ((e = jis.getNextEntry()) != null) {
        if (e.getName().endsWith(".txt")) {
          InputStream is = zipArchive.getInputStream(e);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          while ((nRead = is.read(data, 0, data.length)) != -1)
            baos.write(data, 0, nRead);
          baos.flush();
          try (BufferedReader reader = new BufferedReader(new StringReader(new String(baos.toByteArray())))) {
            String line = reader.readLine();
            while (line != null) {
              int split = line.indexOf(MAP_KV_SPLIT);
              String key = line.substring(0, split);
              String[] values = line.substring(split + MAP_KV_SPLIT.length()).split(MAP_VAL_SPLIT);
              add(key, new HashSet<>(Arrays.asList(values)));
              line = reader.readLine();
            }
          }
        }
      }
    }
  }

  /**
   * Add classes from the given jar to the inheritance graph.
   *
   * @param jar Jar to use.
   * @throws IOException When reading classes from the jar fails.
   */
  public void addJar(File jar) throws IOException {
    JarFile jarArchive = new JarFile(jar);
    try (JarInputStream jis = new JarInputStream(new FileInputStream(jar))) {
      ZipEntry e;
      int nRead;
      byte[] data = new byte[1024];
      while ((e = jis.getNextEntry()) != null) {
        if (e.getName().endsWith(".class")) {
          InputStream is = jarArchive.getInputStream(e);
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          while ((nRead = is.read(data, 0, data.length)) != -1)
            baos.write(data, 0, nRead);
          baos.flush();
          addClass(baos.toByteArray());
        }
      }
    }
  }

  /**
   * Add classes from the given jar to the inheritance graph.
   *
   * @param clazz Class file to use.
   * @throws IOException when the class file cannot be read.
   */
  public void addClass(File clazz) throws IOException {
    addClass(Files.readAllBytes(clazz.toPath()));
  }

  /**
   * Add classes from the given jar to the inheritance graph.
   *
   * @param code Class bytecode.
   */
  public void addClass(byte[] code) {
    Set<String> parents = new HashSet<>();
    ClassReader cr = new ClassReader(code);
    String child = cr.getClassName();
    parents.add(cr.getSuperName());
    parents.addAll(Arrays.asList(cr.getInterfaces()));
    add(child, parents);
  }

  /**
   * Add classes from the given dex file to the inheritance graph.
   *
   * @param dex Dex file to use.
   */
  public void addDex(DexBackedDexFile dex) {
    dex.getClasses().forEach(def -> {
      String child = def.getType();
      Set<String> parents = new HashSet<>(def.getInterfaces());
      parents.add(def.getSuperclass());
      add(child, parents);
    });
  }

  /**
   * Add a child and its parents to the inheritance graph.
   *
   * @param child   A child type.
   * @param parents Set of parents of the child.
   */
  public void add(String child, Set<String> parents) {
    if (child == null || parents == null)
      return;
    parents.remove(null);
    parentsOf.put(child, parents);
    parents.forEach(parent -> childrenOf.putSingle(parent, child));
  }

  /**
   * @param name Internal name of class.
   * @return Direct parents of the class.
   */
  public Set<String> getParents(String name) {
    Set<String> set = parentsOf.get(name);
    if (set == null)
      return Collections.emptySet();
    return set;
  }

  /**
   * @param name Internal name of class.
   * @return All parents of the class.
   */
  public Set<String> getAllParents(String name) {
    Set<String> set = parentsOfCachedAll.get(name);
    if (set == null)
      parentsOfCachedAll.put(name, set = (getParents(name).stream().map(n -> getAllParents(n).stream())
              .reduce(getParents(name).stream(), Stream::concat)).collect(Collectors.toSet()));
    return set;
  }

  /**
   * @param name Internal name of class.
   * @return Direct children of the class.
   */
  public Set<String> getChildren(String name) {
    Set<String> set = childrenOf.get(name);
    if (set == null)
      return Collections.emptySet();
    return set;
  }

  /**
   * @param name Internal name of class.
   * @return All children of the class.
   */
  public Set<String> getAllChildren(String name) {
    Set<String> set = childrenOfCachedAll.get(name);
    if (set == null)
      childrenOfCachedAll.put(name, set = (getChildren(name).stream().map(n -> getAllChildren(n).stream())
              .reduce(getChildren(name).stream(), Stream::concat)).collect(Collectors.toSet()));
    return set;
  }

  /**
   * @return String to write to file for caching purposes.
   */
  public String convertToString() {
    StringBuilder sb = new StringBuilder();
    childrenOf.forEach((parent, children) -> {
      if (parent.equals("java/lang/object"))
        return;
      sb.append(parent).append(MAP_KV_SPLIT).append(String.join(MAP_VAL_SPLIT, children)).append('\n');
    });
    return sb.toString();
  }

  /**
   * Install virtual-value parent check.
   */
  public void install() {
    VirtualValue.setParentCheck((parent, child) ->
            getAllChildren(parent.getInternalName())
                    .contains(child.getInternalName()));
  }
}
