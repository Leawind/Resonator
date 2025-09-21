package io.github.leawind.resonator.server.plugin;

import io.github.leawind.resonator.server.ResonatorServer;
import io.github.leawind.resonator.server.exception.plugin.InvalidPluginException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IResonatorPlugin {
  void setup(ResonatorServer server);

  /// Plugin ID
  String id();

  @Nullable
  ResonatorServer getServer();

  /// [Serializer]
  ///
  ///
  byte[] serialize(@Nullable Object obj);

  /// [Deserializer]
  /// ### Usage
  ///
  /// #### Query parameters
  ///
  /// When receiving a query method request, use this method to deserialize the data.
  ///
  /// Whatever the deserialized data is, it will be passed as the only argument to the query method
  /// handler.
  @Nullable
  Object deserialize(byte[] data);

  Iterable<String> getQueryMethodIds();

  boolean hasQueryMethod(String methodId);

  /// Get query method handler
  ///
  /// ### Returns
  ///
  /// - [QueryMethodHandler] If the method exists
  /// - `null` If the `methodId` is unknown
  @Nullable
  QueryMethodHandler getQueryMethodHandler(String methodId);

  Iterable<String> getEventIds();

  boolean hasEvent(String eventId);

  /// Get subscription handler
  ///
  /// ### Returns
  ///
  /// - [SubscriptionHandler] If the event exists
  /// - `null` If the `eventId` is unknown
  @Nullable
  SubscriptionHandler<?, ?> getSubscriptionHandler(String eventId);

  interface Mutable extends IResonatorPlugin {
    Mutable setSerializer(@Nullable Serializer serializer);

    Mutable setDeserializer(@Nullable Deserializer deserializer);

    /// Register a query method handler
    ///
    /// ### Throws
    ///
    /// - [InvalidPluginException.InvalidQueryMethod] If the query method ID is duplicated
    Mutable registerQueryMethod(String queryMethodId, QueryMethodHandler handler);

    default Mutable registerEvent(String eventId) throws InvalidPluginException {
      return registerEvent(eventId, SubscriptionHandler.DEFAULT);
    }

    /// ### Throws
    ///
    /// - [InvalidPluginException.InvalidEventField] If the event ID is duplicated
    /// - [NullPointerException] If `handler` is `null`
    <C, E> Mutable registerEvent(String eventId, @Nonnull SubscriptionHandler<C, E> handler)
        throws InvalidPluginException, NullPointerException;
  }

  interface Serializer {
    byte[] serialize(Object obj);
  }

  interface Deserializer {
    Object deserialize(byte[] data);
  }

  interface QueryMethodHandler {
    /// Handle a query method request
    ///
    /// ### Parameters
    ///
    /// - `payload` The query method request payload
    ///
    /// ### Returns
    ///
    /// - The query method response payload
    CompletionStage<byte[]> handle(@Nullable byte[] payload);
  }

  /// ### Type parameters
  ///
  /// - `C` Type of processed condition
  /// - `E` Type of raw event data
  interface SubscriptionHandler<Condition, EventInfo> {

    /// Check if the subscription is conditional
    ///
    /// - If true, the client is supposed to send a condition when subscribing. Only events that
    ///   match the condition will be sent to the client.
    /// - If false, whenever an event is published, it will be sent to the client.
    boolean isConditional();

    /// If [#isConditional]:
    ///
    /// Process raw condition data into condition object
    ///
    /// ### Parameters
    ///
    /// - `condition` Raw condition data from client
    ///
    /// ### Returns
    ///
    /// - Condition object
    Condition processCondition(@Nullable byte[] condition);

    /// If [#isConditional]:
    ///
    /// Check if the event data matches the condition
    ///
    /// ### Parameters
    ///
    /// - `condition` Condition object processed by processCondition
    /// - `eventInfo` Event infomation object
    ///
    /// ### Returns
    ///
    /// - `true` if the event data matches the condition
    /// - `false` otherwise
    boolean matches(Condition condition, EventInfo eventInfo);

    SubscriptionHandler<?, ?> DEFAULT =
        new SubscriptionHandler<>() {
          @Override
          public boolean isConditional() {
            return false;
          }

          @Override
          public byte[] processCondition(@Nullable byte[] condition) {
            return condition;
          }

          @Override
          public boolean matches(Object condition, Object eventInfo) {
            return true;
          }
        };
  }
}
