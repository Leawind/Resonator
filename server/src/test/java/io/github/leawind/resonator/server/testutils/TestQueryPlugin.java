package io.github.leawind.resonator.server.testutils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.leawind.resonator.server.ResonatorServer;
import io.github.leawind.resonator.server.plugin.classbased.ResonatorPluginDefinition;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@SuppressWarnings("unused")
@ResonatorPluginDefinition.Meta(id = TestQueryPlugin.ID)
public class TestQueryPlugin extends ResonatorPluginDefinition {
  public static final String ID = "test-query-plugin";

  public final ResonatorServer server;

  public TestQueryPlugin(ResonatorServer server) {
    super();
    this.server = server;
  }

  @Query("list_plugins")
  List<String> listPlugins() {
    return server.listPluginIds();
  }

  @Query("has_plugin")
  boolean hasPlugin(JsonPrimitive pluginId) {
    return server.hasPlugin(pluginId.getAsString());
  }

  @Query
  boolean hasQueryMethod(JsonObject args) {
    var plugin = server.getPlugin(args.get("pluginId").getAsString());
    return plugin != null && plugin.hasQueryMethod(args.get("queryId").getAsString());
  }

  @Query
  static long getTime() {
    return System.currentTimeMillis();
  }

  @Query("to_hex_str")
  static String toHexStr(JsonPrimitive n) {
    return Integer.toHexString(n.getAsInt());
  }

  @Query
  static CompletionStage<String> toHexStrFuture(JsonPrimitive n) {
    return CompletableFuture.completedFuture(Integer.toHexString(n.getAsInt()));
  }
}
