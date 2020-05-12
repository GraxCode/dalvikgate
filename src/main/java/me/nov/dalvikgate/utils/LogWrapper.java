package me.nov.dalvikgate.utils;

import java.util.regex.Matcher;

import org.slf4j.*;

@SuppressWarnings("unused")
public class LogWrapper {
  private static final Logger logfile = LoggerFactory.getLogger("logfile");
  private static final Logger console = LoggerFactory.getLogger("console");

  private StringBuilder sbErr = new StringBuilder();
  private StringBuilder sbInfo = new StringBuilder();

  public void info(String format, Object... args) {
    String msg = compile(format, args);
    console.info(msg);
    logfile.info(msg);
  }

  public void error(String format, Object... args) {
    String msg = compile(format, args);
    console.error(msg);
    logfile.error(msg);
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
    while (msg.contains("{}")) {
      Object arg = args[c];
      String argStr = arg == null ? "null" : arg.toString();
      msg = msg.replaceFirst("\\{}", Matcher.quoteReplacement(argStr));
      c++;
    }
    return msg;
  }
}
