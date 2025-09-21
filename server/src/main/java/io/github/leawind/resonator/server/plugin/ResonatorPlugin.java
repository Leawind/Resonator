package io.github.leawind.resonator.server.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.leawind.resonator.server.ResonatorServer;
import io.github.leawind.resonator.server.exception.plugin.InvalidPluginException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// Implementation of a Resonator plugin
///
/// This class provides a concrete implementation of the IResonatorPlugin interface,
/// managing query methods, events, and serialization/deserialization functionality.
public class ResonatorPlugin implements IResonatorPlugin.Mutable {
  protected static final Gson DEFAULT_GSON = new Gson();
  protected static final Serializer DEFAULT_SERIALIZER = obj -> DEFAULT_GSON.toJson(obj).getBytes();
  protected static final Deserializer DEFAULT_DESERIALIZER =
      data -> {
        var result = DEFAULT_GSON.fromJson(new String(data), JsonElement.class);
        if (result.isJsonArray()) {
          return result.getAsJsonArray().asList().toArray();
        } else {
          return result;
        }
      };
  protected @Nullable ResonatorServer server;

  /// Plugin ID
  protected final String id;

  /// Serializer function for this plugin
  protected @Nullable IResonatorPlugin.Serializer serializer;

  /// Deserializer function for this plugin
  protected @Nullable IResonatorPlugin.Deserializer deserializer;

  /// `MethodId` --> [QueryMethodHandler]
  public final Map<String, QueryMethodHandler> queryMethods = new ConcurrentHashMap<>();

  /// `EventID` --> [SubscriptionHandler]
  ///
  /// The handler must not be `null`
  public final Map<String, SubscriptionHandler<?, ?>> events = new ConcurrentHashMap<>();

  /// Create a new ResonatorPlugin with the given ID
  public ResonatorPlugin(String id) {
    this(id, null, null);
  }

  /// Create a new ResonatorPlugin with the given ID and serialization functions
  protected ResonatorPlugin(
      String id,
      @Nullable IResonatorPlugin.Serializer serializer,
      @Nullable IResonatorPlugin.Deserializer deserializer) {
    this.id = id;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  @Override
  public void setup(ResonatorServer server) {
    if (this.server != null) {
      // TODO
      throw new IllegalStateException(
          String.format("This plugin has already been used by %s", this.server));
    }
    this.server = server;
  }

  @Override
  public @Nullable ResonatorServer getServer() {
    return server;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public byte[] serialize(@Nullable Object obj) {
    return Objects.requireNonNullElse(serializer, DEFAULT_SERIALIZER).serialize(obj);
  }

  @Nullable
  @Override
  public Object deserialize(byte[] data) {
    return Objects.requireNonNullElse(deserializer, DEFAULT_DESERIALIZER).deserialize(data);
  }

  @Override
  public Iterable<String> getQueryMethodIds() {
    return queryMethods.keySet().stream().toList();
  }

  @Override
  public boolean hasQueryMethod(String methodId) {
    return queryMethods.containsKey(methodId);
  }

  @Nullable
  @Override
  public QueryMethodHandler getQueryMethodHandler(String methodId) {
    return queryMethods.get(methodId);
  }

  @Override
  public Iterable<String> getEventIds() {
    return events.keySet().stream().toList();
  }

  @Override
  public boolean hasEvent(String eventId) {
    return events.containsKey(eventId);
  }

  @Nullable
  @Override
  public SubscriptionHandler<?, ?> getSubscriptionHandler(String eventId) {
    return events.get(eventId);
  }

  @Override
  public IResonatorPlugin.Mutable setSerializer(@Nullable IResonatorPlugin.Serializer serializer) {
    this.serializer = serializer;
    return this;
  }

  @Override
  public IResonatorPlugin.Mutable setDeserializer(
      @Nullable IResonatorPlugin.Deserializer deserializer) {
    this.deserializer = deserializer;
    return this;
  }

  @Override
  public IResonatorPlugin.Mutable registerQueryMethod(
      String queryMethodId, QueryMethodHandler handler) {
    if (queryMethods.containsKey(queryMethodId)) {
      throw InvalidPluginException.InvalidQueryMethod.duplicated(queryMethodId);
    }
    queryMethods.put(queryMethodId, handler);
    return this;
  }

  @Override
  public <C, E> IResonatorPlugin.Mutable registerEvent(
      String eventId, @Nonnull SubscriptionHandler<C, E> handler)
      throws InvalidPluginException, NullPointerException {
    if (events.containsKey(eventId)) {
      throw InvalidPluginException.InvalidEventField.duplicated(eventId);
    }
    events.put(eventId, Objects.requireNonNull(handler));
    return this;
  }
}
