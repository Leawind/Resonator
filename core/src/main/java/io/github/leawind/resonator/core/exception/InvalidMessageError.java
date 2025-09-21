package io.github.leawind.resonator.core.exception;

public class InvalidMessageError extends Exception {
  public InvalidMessageError() {
    super();
  }

  public InvalidMessageError(String message) {
    super(message);
  }

  public InvalidMessageError(String message, Throwable cause) {
    super(message, cause);
  }
}
