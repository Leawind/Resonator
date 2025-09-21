package io.github.leawind.resonator.core.utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ByteBufferExtendTest {

  @Test
  public void test() {
    String s1 = "Hello world!";
    int size = s1.getBytes(StandardCharsets.UTF_8).length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(size);

    var eb = new ByteBufferExtend(buffer);
    eb.putI32Utf8(s1);
    eb.raw.flip();
    String s2 = eb.getI32Utf8();
    assert s1.equals(s2);
  }
}
