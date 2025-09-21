package io.github.leawind.resonator.server.plugin.classbased;

import io.github.leawind.resonator.core.utils.ClassInspector;
import io.github.leawind.resonator.core.utils.TypeUtils;
import io.github.leawind.resonator.server.exception.plugin.InvalidPluginException;
import io.github.leawind.resonator.server.plugin.IResonatorPlugin;
import io.github.leawind.resonator.server.plugin.ResonatorPlugin;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;

public final class ClassBasedPluginParser {

  public static <P extends ResonatorPluginDefinition> IResonatorPlugin parse(P definition) {
    return parse(TypeUtils.forceCast(definition.getClass()), definition);
  }

  public static <P extends ResonatorPluginDefinition> IResonatorPlugin parse(
      Class<P> definitionClass, P definition) {
    ClassInspector<P> inspector = ClassInspector.of(definitionClass, definition);

    var meta = parseMeta(definitionClass);
    var plugin =
        new ResonatorPlugin(meta.id())
            .setSerializer(findSerializer(inspector))
            .setDeserializer(findDeserializer(inspector));

    parseQueryMethods(plugin, inspector);
    parseEvents(plugin, inspector);

    definition.setup(plugin);

    return plugin;
  }

  /// ### Throws
  ///
  /// - [InvalidPluginException] If plugin id is not found
  private static <P extends ResonatorPluginDefinition> Meta parseMeta(Class<P> definitionClass)
      throws InvalidPluginException {
    ResonatorPluginDefinition.Meta anno =
        definitionClass.getAnnotation(ResonatorPluginDefinition.Meta.class);
    if (anno != null) {
      return new Meta(anno.id());
    } else {
      var id = definitionClass.getCanonicalName();
      if (id == null) {
        throw new InvalidPluginException(
            "Failed to get plugin ID: Can not get canonical name of anonymous class. Please specify an ID with annotation @ResonatorPluginDefinition.Meta");
      }
      return new Meta(id);
    }
  }

  private static <P extends ResonatorPluginDefinition> void parseQueryMethods(
      IResonatorPlugin.Mutable plugin, ClassInspector<P> inspector) {
    var methods =
        inspector
            .methods()
            .withAnnotation(ResonatorPluginDefinition.Query.class)
            .expect(
                method ->
                    switch (method.getParameterCount()) {
                      case 0, 1 -> true;
                      default -> false;
                    },
                method ->
                    new InvalidPluginException.InvalidQueryMethod(
                        "Query method must have 0 or 1 parameter"));

    methods.forEach(
        method -> {
          var anno = method.getAnnotation(ResonatorPluginDefinition.Query.class);
          String methodId = anno.value().isEmpty() ? method.getName() : anno.value();
          var v = methods.getValueOf(method);
          var returnType = method.getReturnType();

          if (returnType.isAssignableFrom(CompletionStage.class)) {
            // Asynchronous
            if (anno.bytes()) {
              plugin.registerQueryMethod(
                  methodId, argBytes -> TypeUtils.forceCast(v.apply((Object) argBytes)));
            } else {
              plugin.registerQueryMethod(
                  methodId,
                  argBytes -> {
                    Object result;
                    if (argBytes != null && argBytes.length > 0) {
                      result = v.apply(plugin.deserialize(argBytes));
                    } else {
                      result = v.apply();
                    }
                    return TypeUtils.<CompletionStage<Object>>forceCast(result)
                        .thenApply(plugin::serialize);
                  });
            }
          } else {
            // Synchronous
            if (anno.bytes()) {
              plugin.registerQueryMethod(
                  methodId,
                  argBytes ->
                      CompletableFuture.completedStage((byte[]) v.apply((Object) argBytes)));
            } else {
              plugin.registerQueryMethod(
                  methodId,
                  argBytes -> {
                    Object result;
                    if (argBytes != null && argBytes.length > 0) {
                      result = v.apply(plugin.deserialize(argBytes));
                    } else {
                      result = v.apply();
                    }
                    return CompletableFuture.completedStage(plugin.serialize(result));
                  });
            }
          }
        });
  }

  private static <P extends ResonatorPluginDefinition> void parseEvents(
      IResonatorPlugin.Mutable plugin, ClassInspector<P> inspector) throws InvalidPluginException {
    var fields =
        inspector
            .fields()
            .withAnnotation(ResonatorPluginDefinition.Subscribable.class)
            .expectTypeAssignableFrom(
                ResonatorPluginDefinition.Event.class,
                InvalidPluginException.invalidFieldType(ResonatorPluginDefinition.Event.class));
    fields.forEach(
        field -> {
          var anno = field.getAnnotation(ResonatorPluginDefinition.Subscribable.class);
          String eventId = anno.value().isEmpty() ? field.getName() : anno.value();
          ResonatorPluginDefinition.Event<?, ?> event = fields.getValueOf(field);

          if (event == null) {
            throw new InvalidPluginException.InvalidEventField(
                String.format("Non-null value expected at field %s", eventId));
          }

          plugin.registerEvent(eventId, event);
        });
  }

  /// Find serializer function in the definition class
  ///
  /// ### Returns
  ///
  /// - [IResonatorPlugin.Serializer] The serializer function
  /// - `null` If no serializer method/field is found
  ///
  /// ### Throws
  ///
  /// - [InvalidPluginException] If:
  ///   - more than one serializer method/field is found
  ///   - instance is `null` but the serializer method/field is not static
  ///   - invalid type
  private static @Nullable <P extends ResonatorPluginDefinition>
      IResonatorPlugin.Serializer findSerializer(ClassInspector<P> inspector)
          throws InvalidPluginException {
    IResonatorPlugin.Serializer result;

    try {
      // Search in fields
      result =
          inspector
              .fields()
              .withAnnotation(ResonatorPluginDefinition.SerializerFunction.class)
              .expectTypeAssignableTo(
                  IResonatorPlugin.Serializer.class,
                  InvalidPluginException.invalidFieldType(IResonatorPlugin.Serializer.class))
              .getSingleOrThrow(
                  r ->
                      new InvalidPluginException("More than one serializer method/field is found"));

      if (result != null) {
        return result;
      }

      // Search in methods
      {
        var temp =
            inspector
                .methods()
                .withAnnotation(ResonatorPluginDefinition.SerializerFunction.class)
                .expectReturnType(
                    byte[].class, InvalidPluginException.invalidMethodReturnType(byte[].class))
                .expectParametersType(
                    method ->
                        new InvalidPluginException(
                            String.format(
                                "Method '%s' must have one parameter with type Object",
                                method.getName())),
                    Object.class)
                .getSingleOrThrow(
                    r ->
                        new InvalidPluginException(
                            "More than one serializer method/field is found"));
        if (temp != null) {
          result = temp::apply;
        }
      }

    } catch (ClassInspector.InvalidClassException ex) {
      throw new InvalidPluginException(String.format("Invalid member: %s", ex.getMessage()));
    }

    return result;
  }

  /// Find deserializer function in the definition class
  ///
  /// ### Returns
  ///
  /// - [IResonatorPlugin.Deserializer] The deserializer function
  /// - `null` If no deserializer method/field is found
  ///
  /// ### Throws
  ///
  /// - [InvalidPluginException] If:
  ///   - more than one deserializer method/field is found
  ///   - instance is `null` but the deserializer method/field is not static
  ///   - invalid type
  private static <P extends ResonatorPluginDefinition>
      IResonatorPlugin.Deserializer findDeserializer(ClassInspector<P> inspector)
          throws InvalidPluginException {
    IResonatorPlugin.Deserializer result;

    try {
      // Search in fields
      result =
          inspector
              .fields()
              .withAnnotation(ResonatorPluginDefinition.DeserializerFunction.class)
              .expectTypeAssignableTo(
                  IResonatorPlugin.Deserializer.class,
                  InvalidPluginException.invalidFieldType(IResonatorPlugin.Deserializer.class))
              .getSingleOrThrow(
                  r ->
                      new InvalidPluginException(
                          "More than one deserializer method/field is found"));

      if (result != null) {
        return result;
      }

      // Search in methods
      {
        var temp =
            inspector
                .methods()
                .withAnnotation(ResonatorPluginDefinition.DeserializerFunction.class)
                .expectReturnTypeLike(
                    Object.class, InvalidPluginException.invalidMethodReturnType(Object.class))
                .expectParametersType(
                    method ->
                        new InvalidPluginException(
                            String.format(
                                "Method '%s' must have one parameter with type byte[]",
                                method.getName())),
                    byte[].class)
                .getSingleOrThrow(
                    r ->
                        new InvalidPluginException(
                            "More than one deserializer method/field is found"));
        if (temp != null) {
          result = temp::apply;
        }
      }

    } catch (ClassInspector.InvalidClassException ex) {
      throw new InvalidPluginException(String.format("Invalid member: %s", ex.getMessage()));
    }

    return result;
  }

  private record Meta(String id) {}
}
