package io.github.leawind.resonator.core.utils;

public class TypeUtils {
  public static <T> T forceCast(Object from) {
    @SuppressWarnings("unchecked")
    var to = (T) from;
    return to;
  }
}
