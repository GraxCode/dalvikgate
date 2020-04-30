package me.nov.dalvikgate.utils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtils {
  public static String exceptionToString(Throwable t) {
    return Stream.of(t.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining(";"));
  }

}
