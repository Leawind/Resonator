package io.github.leawind.resonator.server.debug;

import io.github.leawind.resonator.server.ResonatorServer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import javax.net.ssl.SSLSession;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DebugWebSocket(WebSocket ws) implements WebSocket {
  public static final Logger LOGGER = LoggerFactory.getLogger("Debug");

  @Override
  public void close(int code, String message) {
    ws.close(code, message);
  }

  @Override
  public void close(int code) {
    ws.close(code);
  }

  @Override
  public void close() {
    ws.close();
  }

  @Override
  public void closeConnection(int code, String message) {
    ws.closeConnection(code, message);
  }

  @Override
  public void send(String text) {
    ws.send(text);
  }

  @Override
  public void send(ByteBuffer buffer) {
    ResonatorServer.Debug.logServerMessage(buffer);
    ws.send(buffer);
  }

  @Override
  public void send(byte[] bytes) {
    ws.send(bytes);
  }

  @Override
  public void sendFrame(Framedata framedata) {
    ws.sendFrame(framedata);
  }

  @Override
  public void sendFrame(Collection<Framedata> frames) {
    ws.sendFrame(frames);
  }

  @Override
  public void sendPing() {
    ws.sendPing();
  }

  @Override
  public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
    ws.sendFragmentedFrame(op, buffer, fin);
  }

  @Override
  public boolean hasBufferedData() {
    return ws.hasBufferedData();
  }

  @Override
  public InetSocketAddress getRemoteSocketAddress() {
    return ws.getRemoteSocketAddress();
  }

  @Override
  public InetSocketAddress getLocalSocketAddress() {
    return ws.getLocalSocketAddress();
  }

  @Override
  public boolean isOpen() {
    return ws.isOpen();
  }

  @Override
  public boolean isClosing() {
    return ws.isClosing();
  }

  @Override
  public boolean isFlushAndClose() {
    return ws.isFlushAndClose();
  }

  @Override
  public boolean isClosed() {
    return ws.isClosed();
  }

  @Override
  public Draft getDraft() {
    return ws.getDraft();
  }

  @Override
  public ReadyState getReadyState() {
    return ws.getReadyState();
  }

  @Override
  public String getResourceDescriptor() {
    return ws.getResourceDescriptor();
  }

  @Override
  public <T> void setAttachment(T attachment) {
    ws.setAttachment(attachment);
  }

  @Override
  public <T> T getAttachment() {
    return ws.getAttachment();
  }

  @Override
  public boolean hasSSLSupport() {
    return ws.hasSSLSupport();
  }

  @Override
  public SSLSession getSSLSession() throws IllegalArgumentException {
    return ws.getSSLSession();
  }

  @Override
  public IProtocol getProtocol() {
    return ws.getProtocol();
  }
}
