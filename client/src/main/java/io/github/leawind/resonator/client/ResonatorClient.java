package io.github.leawind.resonator.client;

import java.net.URI;
import org.java_websocket.client.WebSocketClient;

public class ResonatorClient {
  public WebSocketClient ws;
  private int nextRequestId = 1;

  ResonatorClient(URI uri) {
    ws =
        new WebSocketClient(uri) {
          @Override
          public void onOpen(org.java_websocket.handshake.ServerHandshake handshakedata) {}

          @Override
          public void onMessage(String message) {}

          @Override
          public void onClose(int code, String reason, boolean remote) {}

          @Override
          public void onError(Exception e) {}
        };
  }

  public void connect() {
    ws.connect();
  }

  public static ResonatorClient connect(int port) {
    return connect(URI.create("ws://localhost:" + port));
  }

  public static ResonatorClient connect(URI uri) {
    var cl = new ResonatorClient(uri);
    cl.connect();
    return cl;
  }
}
