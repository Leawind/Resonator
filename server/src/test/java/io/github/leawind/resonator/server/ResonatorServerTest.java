package io.github.leawind.resonator.server;

import io.github.leawind.resonator.server.testutils.TestQueryPlugin;
import io.github.leawind.resonator.server.testutils.TestSerializerPlugin;
import java.net.BindException;
import org.junit.jupiter.api.Test;

public class ResonatorServerTest {
  @Test
  void testPlugins() {
    ResonatorServer server = new ResonatorServer();

    server.plugin(new TestSerializerPlugin());
    server.plugin(new TestQueryPlugin(server));

    {
      assert server.hasPlugin(TestSerializerPlugin.ID);
      server.removePlugin(TestSerializerPlugin.ID);
      assert !server.hasPlugin(TestSerializerPlugin.ID);
      server.plugin(new TestSerializerPlugin());
    }

    {
      assert server.hasPlugin(TestQueryPlugin.ID);
    }
  }

  public static void main(String[] args) throws BindException {
    ResonatorServer server = new ResonatorServer();
    server.plugin(new TestSerializerPlugin());
    server.plugin(new TestQueryPlugin(server));

    server.run(25566);
    assert true;
  }
}
