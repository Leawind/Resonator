package io.github.leawind.resonator.core.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.leawind.resonator.core.message.ServerMessage.Kind;
import org.junit.jupiter.api.Test;

public class ServerMessageTest {
  @Test
  public void testKind() {
    assert Kind.of('O') == Kind.OK;
    assert Kind.of('e') == Kind.EMIT;
    assert Kind.of('E') == Kind.ERROR;
    assert Kind.of(69) == Kind.ERROR;
    assert Kind.of('\0') == Kind.UNKNOWN;
    assert Kind.of('_') == Kind.UNKNOWN;
  }

  @Test
  public void test() {
    assertArrayEquals(new byte[] {'O', 0, 0, (byte) 0xa5}, ServerMessage.ok(0xa5));
  }
}
