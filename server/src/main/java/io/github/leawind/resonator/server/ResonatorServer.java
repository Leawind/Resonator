package io.github.leawind.resonator.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.support.SimpleFunction;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.googlecode.cqengine.query.QueryFactory;
import io.github.leawind.resonator.core.message.ClientMessage;
import io.github.leawind.resonator.core.message.ServerMessage;
import io.github.leawind.resonator.core.utils.AtomicIntSequencer;
import io.github.leawind.resonator.core.utils.TypeUtils;
import io.github.leawind.resonator.server.exception.error.RequestErrors;
import io.github.leawind.resonator.server.exception.plugin.InvalidPluginException;
import io.github.leawind.resonator.server.plugin.IResonatorPlugin;
import io.github.leawind.resonator.server.plugin.classbased.ClassBasedPluginParser;
import io.github.leawind.resonator.server.plugin.classbased.ResonatorPluginDefinition;
import io.github.leawind.resonator.server.utils.IndexedCollectionAdapter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Main server implementation for the Resonator framework
public class ResonatorServer {

  public static final Logger LOGGER =
      LoggerFactory.getLogger(ResonatorServer.class.getSimpleName());

  protected volatile WebSocketServer server;
  protected volatile boolean isRunning = false;
  protected volatile int port = -1;

  /// `Plugin ID` --> [IResonatorPlugin]
  protected final Map<String, IResonatorPlugin> pluginMap = new ConcurrentHashMap<>();

  /// `Client Address` <--> `Client ID`
  ///
  /// #### Insert when
  ///
  /// - a client connected
  ///
  /// #### Remove when
  ///
  /// - client disconnected
  private final BiMap<WebSocket, Integer> clientBiMap = Maps.synchronizedBiMap(HashBiMap.create());
  private final AtomicIntSequencer clientIdSeq =
      AtomicIntSequencer.createRanged(1, 0xFF_FF_FF, i -> !clientBiMap.containsValue(i));

  /// ### Indexed Collection of [SubscriptionRecord]
  ///
  /// Attribute `SUBSCRIPTION_ID` is unique, if you add a record with [IndexedCollection#add] when
  /// there is a duplicate, it will throw [UniqueIndex.UniqueConstraintViolatedException]
  private final IndexedCollectionAdapter<SubscriptionRecord> subscriptions =
      new IndexedCollectionAdapter<>(new ConcurrentIndexedCollection<>());
  private final AtomicIntSequencer subscriptionIdSeq =
      AtomicIntSequencer.createRanged(
          1, 0x7F_FF_FF_FF, id -> subscriptions.queryFirst(SubscriptionRecord.ID, id) == null);

  public ResonatorServer() {
    subscriptions.collection.addIndex(UniqueIndex.onAttribute(SubscriptionRecord.ID));
    subscriptions.collection.addIndex(HashIndex.onAttribute(SubscriptionRecord.CLIENT_ID));
    subscriptions.collection.addIndex(HashIndex.onAttribute(SubscriptionRecord.EVENT_ID));
    subscriptions.collection.addIndex(HashIndex.onAttribute(SubscriptionRecord.PLUGIN_ID));
    subscriptions.collection.addIndex(HashIndex.onAttribute(SubscriptionRecord.HANDLER));
    subscriptions.collection.addIndex(HashIndex.onAttribute(SubscriptionRecord.CONDITION_OBJECT));
  }

  /// Get a plugin by its ID
  @Nullable
  public IResonatorPlugin getPlugin(String pluginId) {
    return pluginMap.get(pluginId);
  }

  /// Check if a plugin with the given ID exists
  public boolean hasPlugin(String pluginId) {
    return pluginMap.containsKey(pluginId);
  }

  /// Register a plugin definition with the server
  public ResonatorServer plugin(ResonatorPluginDefinition definition)
      throws InvalidPluginException {
    return plugin(ClassBasedPluginParser.parse(definition));
  }

  public <P extends ResonatorPluginDefinition> ResonatorServer plugin(IResonatorPlugin plugin) {
    if (hasPlugin(plugin.id())) {
      // TODO plugin version
      // If major version is equal, then use the lager one
      // If major version is not equal, throw error
      LOGGER.warn("Plugin '{}' already registered", plugin.id());
      return this;
    }

    pluginMap.put(plugin.id(), plugin);
    plugin.setup(this);
    return this;
  }

  /// Remove a plugin by its ID
  public void removePlugin(String pluginId) {
    pluginMap.remove(pluginId);
  }

  /// List all registered plugin IDs
  public List<String> listPluginIds() {
    return pluginMap.keySet().stream().sorted().toList();
  }

  /// Get last used port
  ///
  /// If not set yet, returns -1
  public int getPort() {
    return port;
  }

  /// Check if the server is currently running
  public boolean isRunning() {
    return isRunning;
  }

  public static final int DECODER_COUNT = Runtime.getRuntime().availableProcessors() / 2 + 1;

  /// Run the server on the specified port (blocking)
  public void run(int port) throws BindException {
    run(port, DECODER_COUNT);
  }

  /// Run the server on the specified port with a given decoder count (blocking)
  public void run(int port, int decodercount) throws BindException {
    isRunning = true;
    this.port = port;
    server = new Server(port, decodercount);
    server.run();
  }

  /// Start the server on the specified port (non-blocking)
  public void start(int port) throws BindException {
    start(port, DECODER_COUNT);
  }

  /// Start the server on the specified port with a given decoder count (non-blocking)
  ///
  /// ### Throws
  ///
  /// - `BindException`: Server failed to start because of a port conflict
  public synchronized void start(int port, int decodercount) throws BindException {
    isRunning = true;
    this.port = port;
    server = new Server(port, decodercount);
    server.start();
  }

  /// Stop the server
  ///
  /// ### Throws
  ///
  /// - `InterruptedException`: Server failed to stop
  public synchronized void stop() throws InterruptedException {
    isRunning = false;
    server.stop();
  }

  /// Restart server
  ///
  /// ## Throws
  ///
  /// - `InterruptedException`: Server failed to stop
  /// - `BindException`: Server failed to start because of a port conflict
  public void restart(int port, int decoderCount) throws InterruptedException, BindException {
    stop();
    start(port, decoderCount);
  }

  /// Restart server with default decoder count
  ///
  /// ## Throws
  ///
  /// - `InterruptedException`: Server failed to stop
  /// - `BindException`: Server failed to start because of a port conflict
  public void restart(int port) throws InterruptedException, BindException {
    restart(port, DECODER_COUNT);
  }

  /// Broadcast event to all clients that subscribe to the event
  public synchronized <C, E> void publishEvent(
      IResonatorPlugin.SubscriptionHandler<C, E> handler,
      E eventInfo,
      IResonatorPlugin.Serializer dataSerializer) {
    var matchedSubscriptions =
        subscriptions.query(QueryFactory.equal(SubscriptionRecord.HANDLER, handler)).stream()
            .filter(
                subscription ->
                    handler.matches(TypeUtils.forceCast(subscription.conditionObject()), eventInfo))
            .toList();
    if (!matchedSubscriptions.isEmpty()) {
      byte[] data = dataSerializer.serialize(eventInfo);
      matchedSubscriptions.forEach(subscription -> publishEvent(subscription, data));
    }
  }

  private synchronized void publishEvent(SubscriptionRecord subscription, byte[] data) {
    WebSocket ws = clientBiMap.inverse().get(subscription.clientId());
    assert ws != null;
    ws.send(ServerMessage.emit(subscription.id(), data));
  }

  /// Unsubscribe a subscription
  ///
  /// ### Throws
  ///
  /// - [NullPointerException] if id not found
  private synchronized void unsubscribe(int subscriptionId) throws NullPointerException {
    @Nullable
    SubscriptionRecord subscription =
        subscriptions.queryFirst(SubscriptionRecord.ID, subscriptionId);

    if (subscription == null) {
      throw new NullPointerException();
    }

    subscriptions.collection.remove(subscription);
  }

  private class Server extends WebSocketServer {

    public Server(int port, int decodercount) {
      super(new InetSocketAddress(port), decodercount);
      setReuseAddr(true);
    }

    @Override
    public void onStart() {
      LOGGER.info("Service started on port {}", getPort());
    }

    @Override
    public void onOpen(WebSocket ws, ClientHandshake handshake) {
      LOGGER.info(
          "New WebSocket connection: {}, handshake: {}", ws.getRemoteSocketAddress(), handshake);
      clientBiMap.put(ws, clientIdSeq.next());
    }

    @Override
    public void onMessage(WebSocket ws, String msg) {
      LOGGER.debug("Received string message: {}", msg);
    }

    @Override
    public void onMessage(WebSocket ws, ByteBuffer buf) {
      buf.order(ByteOrder.BIG_ENDIAN);

      if (buf.remaining() < 4) {
        LOGGER.warn("Message too short! From {}\n{}", ws.getRemoteSocketAddress(), buf.array());
        return;
      }

      int head = buf.getInt();
      ClientMessage.Kind kind = ClientMessage.Kind.fromHead(head);
      int requestId = head & 0xFFFFFF;

      // Get client ID for this WebSocket connection
      Integer clientIdBoxd = clientBiMap.get(ws);
      int clientId = clientIdBoxd == null ? -1 : clientIdBoxd;

      if (clientId == -1) {
        LOGGER.error("Unregirstered client address: {}", ws.getRemoteSocketAddress());
        return;
      }

      try {
        switch (kind) {
          case HEARTBEAT -> ws.send(ServerMessage.ok(requestId));
          case QUERY -> {
            // Parse body
            ClientMessage.QueryBody body;
            try {
              body = ClientMessage.QueryBody.parse(buf);
            } catch (BufferUnderflowException ex) {
              ws.send(ServerMessage.error(requestId, RequestErrors.Query.invalidBody(ex)));
              break;
            }

            // Get plugin
            @Nullable IResonatorPlugin plugin = getPlugin(body.pluginId());
            if (plugin == null) {
              ws.send(ServerMessage.error(requestId, RequestErrors.unknownPlugin(body.pluginId())));
              break;
            }

            // Get query method handler
            @Nullable
            IResonatorPlugin.QueryMethodHandler handler =
                plugin.getQueryMethodHandler(body.methodId());
            if (handler == null) {
              ws.send(
                  ServerMessage.error(
                      requestId, RequestErrors.Query.unknownMethod(body.methodId())));
              break;
            }

            // Handle query
            handler
                .handle(body.payload())
                .thenAccept(result -> ws.send(ServerMessage.ok(requestId, result)))
                .exceptionally(
                    ex -> {
                      ws.send(ServerMessage.error(requestId, RequestErrors.Query.duringQuery(ex)));
                      return null;
                    });
          }
          case SUBSCRIBE -> {
            ClientMessage.SubscriptionBody body;

            // Parse body
            try {
              body = ClientMessage.SubscriptionBody.parse(buf);
            } catch (BufferUnderflowException ex) {
              ws.send(ServerMessage.error(requestId, RequestErrors.Subscribe.invalidBody(ex)));
              break;
            }

            // Get plugin
            @Nullable IResonatorPlugin plugin = getPlugin(body.pluginId());
            if (plugin == null) {
              ws.send(ServerMessage.error(requestId, RequestErrors.unknownPlugin(body.pluginId())));
              break;
            }

            // Get subscription handler
            @Nullable
            IResonatorPlugin.SubscriptionHandler<?, ?> handler =
                plugin.getSubscriptionHandler(body.eventId());

            // Check if event exists in the plugin
            if (handler == null) {
              ws.send(
                  ServerMessage.error(
                      requestId, RequestErrors.Subscribe.unknownEvent(body.eventId())));
              break;
            }

            // If the handler is conditional, process the condition
            @Nullable
            Object conditionObject =
                handler.isConditional() ? handler.processCondition(body.condition()) : null;

            synchronized (subscriptions) {
              try {

                {
                  SubscriptionRecord existingSubscription;
                  if (handler.isConditional()) {
                    var query4condition =
                        conditionObject == null
                            ? QueryFactory.not(
                                QueryFactory.has(SubscriptionRecord.CONDITION_OBJECT))
                            : QueryFactory.equal(
                                SubscriptionRecord.CONDITION_OBJECT, conditionObject);
                    existingSubscription =
                        subscriptions.queryFirst(
                            QueryFactory.and(
                                QueryFactory.equal(SubscriptionRecord.CLIENT_ID, clientId),
                                QueryFactory.equal(SubscriptionRecord.HANDLER, handler),
                                query4condition));
                  } else {
                    existingSubscription =
                        subscriptions.queryFirst(
                            QueryFactory.and(
                                QueryFactory.equal(SubscriptionRecord.CLIENT_ID, clientId),
                                QueryFactory.equal(SubscriptionRecord.HANDLER, handler)));
                  }

                  if (existingSubscription != null) {
                    ws.send(ServerMessage.okSubscription(requestId, existingSubscription.id()));
                    break;
                  }
                }

                // Create a new subscription ID
                int subscriptionId = subscriptionIdSeq.next();
                // Add subscription record
                subscriptions.collection.add(
                    new SubscriptionRecord(
                        subscriptionId,
                        clientId,
                        body.pluginId(),
                        body.eventId(),
                        handler,
                        conditionObject));

                ws.send(ServerMessage.okSubscription(requestId, subscriptionId));
              } catch (UniqueIndex.UniqueConstraintViolatedException ex) {
                // Create a new subscription ID
                int subscriptionId = subscriptionIdSeq.next();
                ws.send(ServerMessage.okSubscription(requestId, subscriptionId));
              }
            }
          }
          case UNSUBSCRIBE -> {
            if (buf.remaining() < 4) {
              ws.send(ServerMessage.error(requestId, RequestErrors.Unsubscribe.invalidBody()));
              break;
            }

            int subscriptionId = buf.getInt();
            try {
              unsubscribe(subscriptionId);
            } catch (NullPointerException e) {
              ws.send(
                  ServerMessage.error(
                      subscriptionId,
                      RequestErrors.Unsubscribe.unknownSubscription(subscriptionId)));
            }

            ws.send(ServerMessage.ok(subscriptionId));
          }
          case BATCH -> {
            LOGGER.warn("Unimplemented request: Batch\n{}", buf);
          }
          case LIST_PLUGINS -> {
            LOGGER.warn("Unimplemented request: List Plugins\n{}", buf);
          }
          default -> {
            LOGGER.warn("Unknown message kind {}", kind.toByte());
          }
        }
      } catch (RuntimeException ex) {
        LOGGER.error("Internal error:", ex);
        ws.send(ServerMessage.error(requestId, RequestErrors.internal(ex)));
      }
    }

    @Override
    public void onError(WebSocket ws, Exception e) {
      LOGGER.error("WebSocket Error: {}\n{}", e.getMessage(), e.toString());
    }

    /// When a client disconnected
    ///
    /// - Remove all subscriptions of this client
    @Override
    public void onClose(WebSocket ws, int code, String reason, boolean remote) {
      synchronized (ResonatorServer.this) {
        LOGGER.info(
            "WebSocket connection closed: {}, Code: {}, Reason: {}",
            ws.getRemoteSocketAddress(),
            code,
            reason);

        Integer clientId = clientBiMap.get(ws);
        if (clientId == null) {
          LOGGER.error("Unregistered client: {}", ws);
          return;
        }

        var toRemove = subscriptions.query(SubscriptionRecord.CLIENT_ID, clientId);

        // Remove all subscriptions of this client
        toRemove.forEach(subscriptions.collection::remove);

        clientBiMap.remove(ws);
      }
    }
  }

  public static final class Debug {
    public static final Logger LOGGER = LoggerFactory.getLogger("Debug");

    public static void logClientMessage(ByteBuffer buffer) {
      int head = buffer.getInt(0);
      ClientMessage.Kind kind = ClientMessage.Kind.fromHead(head);
      int id = head & 0xFFFFFF;
      byte[] bytes = new byte[buffer.capacity() - 4];
      buffer.get(4, bytes);
      LOGGER.info("ClientMessage: {} [{}] {}", kind, id, new String(bytes, StandardCharsets.UTF_8));
    }

    public static void logServerMessage(ByteBuffer buffer) {
      int head = buffer.getInt(0);
      ServerMessage.Kind kind = ServerMessage.Kind.fromHead(head);
      if (kind == ServerMessage.Kind.UNKNOWN) {
        LOGGER.info("Unknown???");
      }
      int id = head & 0xFFFFFF;
      byte[] bytes = new byte[buffer.capacity() - 4];
      buffer.get(4, bytes);
      LOGGER.info("ServerMessage: {} [{}] {}", kind, id, new String(bytes, StandardCharsets.UTF_8));
    }
  }

  /// ### Fields / Parameters
  ///
  /// - `id` Subscription ID
  /// - `clientId` See [#clientBiMap]
  /// - `pluginId` See [#pluginMap]
  /// - `eventId` Event ID in the plugin
  /// - `handler` Subscription handler
  /// - `conditionObject` Condition object processed by the handler
  ///
  /// ### Refer
  ///
  /// - [ResonatorServer#subscriptions]
  private record SubscriptionRecord(
      int id,
      int clientId,
      String pluginId,
      String eventId,
      IResonatorPlugin.SubscriptionHandler<?, ?> handler,
      @Nullable Object conditionObject) {

    static final Attribute<SubscriptionRecord, Integer> ID =
        attr(Integer.class, "id", SubscriptionRecord::id);
    static final Attribute<SubscriptionRecord, Integer> CLIENT_ID =
        attr(Integer.class, "clientId", SubscriptionRecord::clientId);
    static final Attribute<SubscriptionRecord, String> PLUGIN_ID =
        attr(String.class, "pluginId", SubscriptionRecord::pluginId);
    static final Attribute<SubscriptionRecord, String> EVENT_ID =
        attr(String.class, "eventId", SubscriptionRecord::eventId);
    static final Attribute<SubscriptionRecord, Object> HANDLER =
        attr(Object.class, "handler", SubscriptionRecord::handler);
    static final Attribute<SubscriptionRecord, Object> CONDITION_OBJECT =
        QueryFactory.nullableAttribute(
            SubscriptionRecord.class,
            Object.class,
            "conditionObject",
            SubscriptionRecord::conditionObject);

    private static <A> Attribute<SubscriptionRecord, A> attr(
        Class<A> attributeType, String name, SimpleFunction<SubscriptionRecord, A> function) {
      return QueryFactory.attribute(SubscriptionRecord.class, attributeType, name, function);
    }
  }
}
