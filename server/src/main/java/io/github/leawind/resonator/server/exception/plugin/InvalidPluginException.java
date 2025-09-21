package io.github.leawind.resonator.server.exception.plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

public class InvalidPluginException extends RuntimeException {

  protected InvalidPluginException() {
    super();
  }

  public InvalidPluginException(String message) {
    super(message);
  }

  protected InvalidPluginException(String message, Throwable cause) {
    super(message, cause);
  }

  protected InvalidPluginException(Throwable cause) {
    super(cause);
  }

  public static Function<Field, Throwable> invalidFieldType(Class<?> expectedType) {
    return field ->
        new InvalidPluginException(
            String.format(
                "Field %s must be of type %s, got %s",
                field.getName(),
                expectedType.getCanonicalName(),
                field.getType().getCanonicalName()));
  }

  public static Function<Method, Throwable> invalidMethodReturnType(Class<?> expectedType) {
    return method ->
        new InvalidPluginException(
            String.format(
                "Method %s must return %s, got %s",
                method.getName(),
                expectedType.getCanonicalName(),
                method.getReturnType().getCanonicalName()));
  }

  public static class InvalidQueryMethod extends InvalidPluginException {

    public InvalidQueryMethod(String message) {
      super(message);
    }

    public static InvalidQueryMethod duplicated(String methodId) {
      return new InvalidQueryMethod("Duplicated query method: " + methodId);
    }
  }

  public static class InvalidEventField extends InvalidPluginException {

    public InvalidEventField(String message) {
      super(message);
    }

    public static InvalidEventField duplicated(String eventId) {
      return new InvalidEventField("Duplicated event: " + eventId);
    }
  }
}
