package me.nov.dalvikgate.logging;

import java.util.regex.Matcher;

import org.slf4j.*;

public class LogWrapper {
  private final Logger logfile = LoggerFactory.getLogger("logfile");
  private Logger console = LoggerFactory.getLogger("console");

  public void info(String format, Object... args) {
    String msg = compile(format, args);
    console.info(msg);
    logfile.info(msg);
  }

  public void errorIf(String format, boolean error, Object... args) {
    if (error) {
      error(format, args);
    } else {
      info(format, args);
    }
  }

  public void warning(String format, Object... args) {
    String msg = compile(format, args);
    console.warn(msg);
    logfile.warn(msg);
  }

  public void error(String format, Object... args) {
    String msg = compile(format, args);
    console.error(msg);
    logfile.error(msg);
  }

  public void error(String format, Throwable t, Object... args) {
    String msg = compile(format, args);
    console.error(msg, t);
    logfile.error(msg, t);
  }

  public void debug(String format, Object... args) {
    String msg = compile(format, args);
    console.debug(msg);
    logfile.debug(msg);
  }

  public void trace(String format, Object... args) {
    String msg = compile(format, args);
    console.trace(msg);
    logfile.trace(msg);
  }

  public void disableConsoleLog() {
    console = LoggerFactory.getLogger("null");
  }

  /**
   * Compiles message with "{}" arg patterns.
   *
   * @param msg  Message pattern.
   * @param args Values to pass.
   * @return Compiled message with inlined arg values.
   */
  private static String compile(String msg, Object[] args) {
    int c = 0;
    try {
      while (msg.contains("{}")) {
        Object arg = args[c];
        String argStr = arg == null ? "null" : arg.toString();
        msg = msg.replaceFirst("\\{}", Matcher.quoteReplacement(argStr));
        c++;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    return msg;
  }

}