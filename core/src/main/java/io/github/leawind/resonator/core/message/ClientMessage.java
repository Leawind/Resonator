package io.github.leawind.resonator.core.message;

import com.google.gson.*;
import io.github.leawind.resonator.core.utils.ByteBufferExtend;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/// Client message types and parsing utilities
///
/// This class provides utilities for handling messages sent from clients to the server,
/// including message kinds, body parsing, and payload handling.
public final class ClientMessage {

  private ClientMessage() {}

  /// Message sended by the client to the server.
  ///
  /// See [ServerMessage.Kind]
  public enum Kind {
    /// Heartbeat message
    HEARTBEAT('H'),
    /// Query something and expect a response
    ///
    /// ### Ok
    ///
    /// ### Error
    ///
    /// - If failed to parse body as [ClientMessage.QueryBody]
    /// - If plugin is unknown
    /// - If method is unknown
    /// - If error occurred handling the query
    QUERY('Q'),
    /// Subscribe from a client to a event in a plugin
    ///
    /// If the same client and event already subscribed, return existing subscription ID. Otherwise,
    /// create a new subscription.
    ///
    /// ## Responses
    ///
    /// ### Ok
    ///
    /// - Existing subscription ID
    /// - New subscription ID
    ///
    /// ### Error
    ///
    /// - If failed to parse body as [ClientMessage.SubscriptionBody]
    /// - If plugin is unknown
    /// - If event is unknown
    SUBSCRIBE('S'),
    /// Cancel a subscription
    ///
    /// ## Response
    ///
    /// ### Ok
    ///
    /// - If the subscription is canceled successfully
    ///
    /// ### Error
    ///
    /// - If failed to parse body as subscription ID (i32)
    /// - If the subscription is not found
    UNSUBSCRIBE('U'),
    /// List plugins
    LIST_PLUGINS('L'),
    /// Batch request
    BATCH('B'),
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
        case 'H' -> HEARTBEAT;
        case 'Q' -> QUERY;
        case 'L' -> LIST_PLUGINS;
        case 'S' -> SUBSCRIBE;
        case 'U' -> UNSUBSCRIBE;
        case 'B' -> BATCH;
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

  /// Body of a query message
  public record QueryBody(
      /// Plugin ID
      String pluginId,
      /// Method ID
      String methodId,
      /// Payload
      byte[] payload) {

    /// ### Throws
    ///
    /// - [BufferUnderflowException] If:
    ///   - There are not enough bytes in this buffer
    ///   - Arguments data is not a valid representation for a [JsonArray]
    public static QueryBody parse(ByteBuffer buffer) throws BufferUnderflowException {
      ByteBufferExtend buf = new ByteBufferExtend(buffer);

      String pluginId, methodId;
      pluginId = buf.getI32Utf8();
      methodId = buf.getI32Utf8();

      int remaining = buffer.remaining();
      byte[] argument = new byte[remaining];
      if (remaining != 0) {
        buffer.get(argument);
      }

      return new QueryBody(pluginId, methodId, argument);
    }
  }

  /// Body of a subscription message
  public record SubscriptionBody(
      /// Plugin ID
      String pluginId,
      ///  Event name
      String eventId,
      byte[] condition) {

    /// ### Throws
    ///
    /// - [BufferUnderflowException] If:
    ///   - There are not enough bytes in this buffer
    public static SubscriptionBody parse(ByteBuffer buffer) throws BufferUnderflowException {
      ByteBufferExtend buf = new ByteBufferExtend(buffer);

      String pluginId, eventId;
      pluginId = buf.getI32Utf8();
      eventId = buf.getI32Utf8();

      int remaining = buffer.remaining();
      byte[] condition = new byte[remaining];
      if (remaining != 0) {
        buffer.get(condition);
      }
      return new SubscriptionBody(pluginId, eventId, condition);
    }
  }
}
