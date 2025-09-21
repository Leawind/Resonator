package io.github.leawind.resonator.server.testutils;

import com.google.gson.JsonPrimitive;
import io.github.leawind.resonator.server.ResonatorServer;
import io.github.leawind.resonator.server.plugin.classbased.ResonatorPluginDefinition;

@SuppressWarnings("unused")
@ResonatorPluginDefinition.Meta(id = TestEventPlugin.ID)
public class TestEventPlugin extends ResonatorPluginDefinition {
  public static final String ID = "test-event-plugin";
  public final ResonatorServer server;

  public TestEventPlugin(ResonatorServer server) {
    this.server = server;
  }

  @Subscribable Emitable<Void> onSimpleEvent = event();

  @Query
  public void simpleEvent() {
    onSimpleEvent.emit(null);
  }

  @Subscribable
  Emitable<Integer> onEncounterInteger =
      event(
          (JsonPrimitive condition, Integer info) -> {
            int i = condition.getAsInt();
            return info % i == 0;
          });

  @Query
  public void encounterInteger(JsonPrimitive i) {
    onEncounterInteger.emit(i.getAsInt());
  }
}
