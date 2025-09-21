package io.github.leawind.resonator.core.message;

import io.github.leawind.resonator.core.exception.error.RequestError;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

/// Server message types and creation utilities
///
/// This class provides utilities for creating messages sent from the server to clients,
/// including success responses, error responses, and event emissions.
public final class ServerMessage {
  private ServerMessage() {}

  /// Message sent from the server to the client
  ///
  /// See [ClientMessage.Kind]
  public enum Kind {
    /// Success response to a client message
    OK('O'),
    /// Error occured
    ERROR('E'),
    /// Emit a eventId
    EMIT('e'),
    UNKNOWN('\0');

    private final char identifier;

    Kind(char ch) {
      this.identifier = ch;
    }

    /// Get the byte representation of this message kind
    public byte toByte() {
      return (byte) identifier;
    }

    /// Get the char representation of this message kind
    public char toChar() {
      return identifier;
    }

    /// Get message kind from char identifier
    public static Kind of(char id) {
      return switch (id) {
        case 'O' -> OK;
        case 'E' -> ERROR;
        case 'e' -> EMIT;
        default -> UNKNOWN;
      };
    }

    /// Get message kind from byte identifier
    public static Kind of(byte id) {
      return of((char) id);
    }

    /// Get message kind from int identifier
    public static Kind of(int id) {
      return of((char) id);
    }

    /// Get message kind from message head
    public static Kind fromHead(int head) {
      return of((head & 0xFF000000) >> 24);
    }
  }

  private static ByteBuffer create(Kind kind, int u24, int bodySize) {
    return ByteBuffer.allocate(4 + bodySize)
        .order(ByteOrder.BIG_ENDIAN)
        .putInt((kind.toByte() << 24) | u24);
  }

  /// Create an OK response with no body
  public static byte[] ok(int requestId) {
    return create(Kind.OK, requestId, 0).array();
  }

  /// Create an OK response with a subscription ID
  public static byte[] okSubscription(int requestId, int subscriptionId) {
    return create(Kind.OK, requestId, 4).putInt(subscriptionId).array();
  }

  /// Create an OK response with a body
  public static byte[] ok(int requestId, @Nullable byte[] body) {
    if (body == null || body.length == 0) {
      return ok(requestId);
    }
    return create(Kind.OK, requestId, body.length).put(body).array();
  }

  /// Create an error response with an error code
  public static byte[] error(int requestId, byte code) {
    return create(Kind.ERROR, requestId, 1).put(code).array();
  }

  /// Create an error response from a RequestError
  public static byte[] error(int requestId, RequestError ex) {
    return error(requestId, ex.code(), ex.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] error(int requestId, byte code, @Nullable byte[] payload) {
    if (payload == null || payload.length == 0) {
      return error(requestId, code);
    }
    return create(Kind.ERROR, requestId, 1 + payload.length).put(code).put(payload).array();
  }

  /// Create an emit message with a subscription ID
  public static byte[] emit(int subscriptionId) {
    return create(Kind.EMIT, subscriptionId, 4).putInt(subscriptionId).array();
  }

  /// Create an emit message with a subscription ID and arguments
  public static byte[] emit(int subscriptionId, @Nullable byte[] args) {
    if (args == null || args.length == 0) {
      return emit(subscriptionId);
    }
    return create(Kind.EMIT, subscriptionId, args.length).put(args).array();
  }
}
