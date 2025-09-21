package io.github.leawind.resonator.server.exception.error;

import io.github.leawind.resonator.core.exception.error.RequestError;
import java.nio.BufferUnderflowException;

/// Factory for [RequestError]
public final class RequestErrors {
  public static RequestError internal(Exception ex) {
    return new RequestError(0x01, ex);
  }

  public static RequestError unknownPlugin(String pluginId) {
    return new RequestError(0x02, String.format("Unknown plugin id: %s", pluginId));
  }

  public static final class Query {
    public static RequestError invalidBody(BufferUnderflowException ex) {
      return new RequestError(0x10, ex);
    }

    public static RequestError unknownMethod(String methodId) {
      return new RequestError(0x11, String.format("Unknown query method id: %s", methodId));
    }

    public static RequestError duringQuery(Throwable cause) {
      return new RequestError(0x12, cause);
    }
  }

  public static final class Subscribe {
    public static RequestError invalidBody(BufferUnderflowException ex) {
      return new RequestError(0x20, ex);
    }

    public static RequestError unknownEvent(String eventId) {
      return new RequestError(0x21, String.format("Unknown event id: %s", eventId));
    }
  }

  public static final class Unsubscribe {

    public static RequestError invalidBody() {
      return new RequestError(0x30);
    }

    public static RequestError unknownSubscription(int subscriptionId) {
      return new RequestError(0x31, String.format("Unknown subscription id: %d", subscriptionId));
    }
  }
}
