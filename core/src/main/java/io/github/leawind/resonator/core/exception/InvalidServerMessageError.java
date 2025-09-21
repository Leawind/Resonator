package io.github.leawind.resonator.core.exception;

public class InvalidServerMessageError extends InvalidMessageError {

  public InvalidServerMessageError(String message) {
    super(message);
  }

  public InvalidServerMessageError(String message, Throwable cause) {
    super(message, cause);
  }
}
