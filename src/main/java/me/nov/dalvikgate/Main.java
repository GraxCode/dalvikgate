package me.nov.dalvikgate;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.*;

/**
 * Separate class for command line inputs, as the CLI library is not a required dependency by users. This allows users to use DalvikGate as a lib without the CLI.
 */
public class Main implements Callable<Void> {
  @Parameters(index = "0", paramLabel = "input", description = "the dalvik file to convert (dex/odex/apk)")
  public File input;

  @Parameters(index = "1", paramLabel = "output", description = "the output jar archive")
  public File output;

  @Option(names = { "-nr", "--noresolve" }, description = "do not resolve variable types")
  public boolean noResolve;

  @Option(names = { "-no", "--nooptimize" }, description = "do not optimize produced code")
  public boolean noOptimize;

  @Option(names = { "-nf", "--namefilter" }, description = "regex whitelist filter", defaultValue = ".*")
  public String nameFilter;

  @Option(names = { "-v", "--verbose" }, description = "print exceptions")
  public boolean verbose;

  public static void main(String[] args) {
    new CommandLine(new Main()).execute(args);
  }

  @Override
  public Void call() throws Exception {
    DexToASM.nameFilter = nameFilter;
    DexToASM.noOptimize = noOptimize;
    DexToASM.noResolve = noResolve;
    if (verbose)
      DexToASM.dex2Jar(input, output);
    else
      DexToASM.dex2JarConsole(input, output);
    return null;
  }
}
