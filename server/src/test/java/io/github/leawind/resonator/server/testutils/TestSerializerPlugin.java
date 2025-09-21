package io.github.leawind.resonator.server.testutils;

import com.google.gson.*;
import io.github.leawind.resonator.server.plugin.classbased.ResonatorPluginDefinition;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@ResonatorPluginDefinition.Meta(id = TestSerializerPlugin.ID)
public final class TestSerializerPlugin extends ResonatorPluginDefinition {
  public static final String ID = "test-serializer-plugin";

  /// Custom serialization method
  @SerializerFunction
  static byte[] serializer(Object obj) {
    return new Gson().toJson(obj).getBytes();
  }

  /// Custom deserialization method
  @DeserializerFunction
  static JsonElement deserializer(byte[] bytes) {
    return new Gson().fromJson(new String(bytes), JsonElement.class);
  }

  @Query("get_time")
  static long getTime() {
    return System.currentTimeMillis();
  }

  @Query("to_hex_str")
  static String toHexStr(JsonPrimitive n) {
    return Integer.toHexString(n.getAsInt());
  }

  @Query("to_hex_str_future")
  static CompletionStage<String> toHexStrFuture(JsonPrimitive n) {
    return CompletableFuture.completedFuture(Integer.toHexString(n.getAsInt()));
  }
}
