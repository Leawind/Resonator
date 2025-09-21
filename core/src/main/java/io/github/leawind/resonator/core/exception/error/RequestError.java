package io.github.leawind.resonator.core.exception.error;

public class RequestError extends Exception {
  public final byte code;

  public RequestError(int code) {
    this.code = (byte) code;
  }

  public RequestError(int code, String message) {
    super(message);
    this.code = (byte) code;
  }

  public RequestError(int code, String message, Throwable cause) {
    super(message, cause);
    this.code = (byte) code;
  }

  public RequestError(int code, Throwable cause) {
    super(cause);
    this.code = (byte) code;
  }

  public final byte code() {
    return code;
  }
}
