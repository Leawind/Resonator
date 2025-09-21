package io.github.leawind.resonator.server.plugin.classbased;

import io.github.leawind.resonator.core.utils.TypeUtils;
import io.github.leawind.resonator.server.plugin.IResonatorPlugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;

public abstract class ResonatorPluginDefinition {
  /// ### See
  ///
  /// - [#setup]
  private @Nullable IResonatorPlugin plugin = null;

  protected ResonatorPluginDefinition() {}

  /// This method will be called once this definition class is parsed.
  ///
  /// ### See
  ///
  /// - [ClassBasedPluginParser#parse(Class, ResonatorPluginDefinition)]
  void setup(IResonatorPlugin plugin) {
    this.plugin = plugin;
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Meta {
    /// Plugin ID
    ///
    /// Default value is empty string, which means the ID is the same as the full class name.
    ///
    /// ### Example
    ///
    /// In this example, the pluginId ID is `com.foo.bar.ExamplePlugin`
    ///
    /// ```java
    /// package com.foo.bar;
    ///
    /// @ResonatorPlugin.Meta()
    /// class ExamplePlugin extends ResonatorPlugin {}
    /// ```
    String id() default "";
  }

  /// Default instance of this pluginId
  ///
  /// - Must be used on `static` method or field
  /// - The type or return type must derive from [ResonatorPluginDefinition]
  /// - Can occur at most once in a custom plugin class
  ///
  /// @deprecated
  @Target({ElementType.METHOD, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DefaultInstance {}

  /// This annotation is used to mark a serializer.
  ///
  /// This annotation can be used on:
  ///
  /// - Field of type [IResonatorPlugin.Serializer]
  /// - Method with 1 parameter of type `Object`, and return type `byte[]`
  ///
  /// The parsed function will be used on:
  ///
  /// - The return value of query method
  /// - The event data when pushing event
  @Target({ElementType.METHOD, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SerializerFunction {}

  /// This annotation is used to mark a deserializer.
  ///
  /// This annotation can be used on:
  ///
  /// - Field of type [IResonatorPlugin.Deserializer]
  /// - Method with 1 parameter of type `byte[]`, and return type `Object`
  ///
  /// The parsed function will be used on:
  ///
  /// - The arguments of query method
  @Target({ElementType.METHOD, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DeserializerFunction {}

  /// Mark a eventId as query eventId
  ///
  /// - The annotated method must have zero or one parameter
  /// - If the return type is [CompletionStage], the query will be async.
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Query {
    /// ## Query Method ID
    ///
    /// Query eventId ID must be unique in a pluginId. Method overloading is forbidden!
    ///
    /// If empty (default), eventId name will be used.
    String value() default "";

    /// ## If `false` (default)
    ///
    /// Use specific serializer and deserializer.
    ///
    /// ## If `true`
    ///
    /// Directly use `byte[]` as the arguments and return type.
    ///
    /// ### Example
    ///
    /// ```java
    ///   @Query(bytes = true)
    ///   byte[] toOctalStr(byte[] payload) {
    ///     var buf = ByteBuffer.wrap(payload);
    ///     int n = buf.getInt();
    ///     String s = Integer.toOctalString(n);
    ///     return new Gson().toJson(s).getBytes();
    ///   }
    /// ```
    boolean bytes() default false;
  }

  /// Mark a subscribable event
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Subscribable {
    /// ## Subscribe Event ID
    ///
    /// Subscribe eventId ID must be unique
    String value() default "";
  }

  /// Create a unconditional event
  ///
  /// Whoever subscribe to this event will receive the event data once it is published.
  protected <C, E> Emitable<E> event() {
    return new Event<>(null, null);
  }

  /// Create a conditional event
  ///
  /// Use the plugin deserializer to deserialize the condition data received from the client.
  protected <C, E> Emitable<E> event(@Nullable BiFunction<C, E, Boolean> conditionMatcher) {
    return new Event<>(
        conditionMatcher,
        data -> TypeUtils.forceCast(Objects.requireNonNull(plugin).deserialize(data)));
  }

  /// Create a conditional event
  protected <C, E> Emitable<E> event(
      @Nullable BiFunction<C, E, Boolean> conditionMatcher,
      @Nullable Function<byte[], C> conditionProcessor) {
    return new Event<>(conditionMatcher, conditionProcessor);
  }

  protected interface Emitable<E> {
    void emit(E eventInfo);
  }

  protected final class Event<C, E>
      implements IResonatorPlugin.SubscriptionHandler<C, E>, Emitable<E> {
    @Nullable private final BiFunction<C, E, Boolean> conditionMatcher;
    @Nullable private final Function<byte[], C> conditionProcessor;

    protected Event(
        @Nullable BiFunction<C, E, Boolean> conditionMatcher,
        @Nullable Function<byte[], C> conditionProcessor) {
      this.conditionMatcher = conditionMatcher;
      this.conditionProcessor = conditionProcessor;
    }

    @Override
    public boolean isConditional() {
      return conditionProcessor != null && conditionMatcher != null;
    }

    @Override
    public C processCondition(@Nullable byte[] condition) {
      assert conditionProcessor != null;
      return conditionProcessor.apply(condition);
    }

    @Override
    public boolean matches(C condition, E eventInfo) {
      assert conditionMatcher != null;
      return conditionMatcher.apply(condition, eventInfo);
    }

    @Override
    public void emit(E eventInfo) {
      if (plugin == null) {
        throw new IllegalStateException("plugin definition has not been setup");
      }
      var server = plugin.getServer();
      if (server == null) {
        throw new IllegalStateException("plugin has not been setup");
      }
      plugin.getServer().publishEvent(this, eventInfo, plugin::serialize);
    }

    @Nullable
    public BiFunction<C, E, Boolean> conditionMatcher() {
      return conditionMatcher;
    }

    @Nullable
    public Function<byte[], C> conditionProcessor() {
      return conditionProcessor;
    }
  }
}
