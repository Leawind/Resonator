package io.github.leawind.resonator.core.utils;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;

/// Extension utilities for ByteBuffer operations
///
/// Provides convenient methods for working with ByteBuffer, particularly
/// for reading and writing UTF-8 strings with length prefixes.
public class ByteBufferExtend {
  /// The underlying ByteBuffer
  public final ByteBuffer raw;

  /// Create a new ByteBufferExtend wrapper
  public ByteBufferExtend(ByteBuffer buffer) {
    raw = buffer;
  }

  /// ### Throws
  ///
  /// - [BufferOverflowException] If there is insufficient space in this buffer
  /// - [ReadOnlyBufferException] If this buffer is read-only
  public void putI32Utf8(String s) throws BufferOverflowException, ReadOnlyBufferException {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    raw.putInt(bytes.length);
    raw.put(bytes);
  }

  /// ### Throws
  ///
  /// - [BufferUnderflowException] If there are not enough bytes in this buffer
  public String getI32Utf8() throws BufferUnderflowException {
    int size = raw.getInt();
    byte[] bytes = new byte[size];
    raw.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}