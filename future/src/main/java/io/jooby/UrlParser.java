package io.jooby;

import org.jooby.funzy.Throwing;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public final class UrlParser {
  private static final char SPACE = 0x20;

  public static QueryString queryString(String path) {
    if (path == null) {
      return QueryString.EMPTY;
    }
    int start = path.indexOf('?');
    if (start < 0) {
      return QueryString.EMPTY;
    }
    int len = path.length();
    QueryString result = new QueryString(path.substring(start));
    parse(path, start + 1, len, result);
    return result;
  }

  private static void parse(String source, int start, int length, Value.Object root) {
    int nameStart = start;
    int nameEnd = length;
    // %00 size = 3
    int decodedSize = (length - start) / 3;
    StringBuilder decodedBuffer = new StringBuilder(decodedSize);
    ByteBuffer decoderInput = ByteBuffer.allocate(decodedSize);
    CharBuffer decoderOutput = CharBuffer.allocate(decodedSize);
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
    for (int i = nameStart; i < length; i++) {
      char ch = source.charAt(i);
      if (ch == '=') {
        // Parameter name ready
        nameEnd = i;
      } else if (ch == '&' || ch == ';') {
        newParam(root, source, nameStart, nameEnd, nameEnd + 1, i, decodedBuffer, decoderInput,
            decoderOutput, decoder);
        nameStart = i + 1;
      }
    }
    newParam(root, source, nameStart, nameEnd, nameEnd + 1, length, decodedBuffer, decoderInput,
        decoderOutput, decoder);
  }

  private static void newParam(Value.Object root, String source, int nameStart, int nameEnd,
      int valueStart, int valueEnd, StringBuilder decodedBuffer,
      ByteBuffer decoderInput, CharBuffer decoderOutput, CharsetDecoder decoder) {
    if (nameStart < valueEnd) {
      // find target
      Value.Object target = root;
      for (int i = nameStart; i < nameEnd; i++) {
        char ch = source.charAt(i);
        if (ch == '.') {
          String name = decode(source, nameStart, i, decodedBuffer, decoderInput, decoderOutput,
              decoder);
          nameStart = i + 1;
          target = target.getOrCreateScope(name);
        } else if (ch == '[') {
          if (nameStart < i) {
            String name = decode(source, nameStart, i, decodedBuffer, decoderInput, decoderOutput,
                decoder);
            target = target.getOrCreateScope(name);
          }
          nameStart = i + 1;
        } else if (ch == ']') {
          if (i + 1 < nameEnd) {
            String name = decode(source, nameStart, i, decodedBuffer, decoderInput, decoderOutput,
                decoder);
            nameStart = i + 1;
            target = target.getOrCreateScope(name);
          } else {
            nameEnd = i;
          }
        }
      }
      String name = decode(source, nameStart, nameEnd, decodedBuffer, decoderInput, decoderOutput,
          decoder);
      for (int i = valueStart; i < valueEnd; i++) {
        char ch = source.charAt(i);
        if (ch == ',') {
          String value = decode(source, valueStart, i, decodedBuffer, decoderInput, decoderOutput,
              decoder);
          target.put(name, value);
          valueStart = i + 1;
        }
      }
      String value = decode(source, valueStart, valueEnd, decodedBuffer, decoderInput,
          decoderOutput, decoder);
      target.put(name, value);
    }
  }

  public static String decodePath(String path) {
    // %00 size = 3
    int len = path.length();
    int decodedSize = len / 3;
    StringBuilder decodedBuffer = new StringBuilder(decodedSize);
    ByteBuffer decoderInput = ByteBuffer.allocate(decodedSize);
    CharBuffer decoderOutput = CharBuffer.allocate(decodedSize);
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
    return decode(path, 0, len, decodedBuffer, decoderInput, decoderOutput, decoder, false);
  }

  private static String decode(String source, int start, int len, StringBuilder decodedBuffer,
      ByteBuffer decoderInput, CharBuffer decoderOutput, CharsetDecoder decoder) {
    return decode(source, start, len, decodedBuffer, decoderInput, decoderOutput, decoder, true);
  }

  private static String decode(String source, int start, int len, StringBuilder decodedBuffer,
      ByteBuffer decoderInput, CharBuffer decoderOutput, CharsetDecoder decoder,
      boolean encodePlus) {
    decodedBuffer.setLength(0);
    for (int i = start; i < len; i++) {
      char ch = source.charAt(i);
      if (ch == '%') {
        decodedBuffer.append(source, start, i);
        i = decodePercent(source, i, len, decoderInput, decoderOutput, decoder);
        decodedBuffer.append(decoderOutput.flip().toString());
        start = i + 1;
      } else if (ch == '+' && encodePlus) {
        decodedBuffer.append(source, start, i);
        decodedBuffer.append(SPACE);
        start = i + 1;
      }
    }
    if (decodedBuffer.length() > 0) {
      decodedBuffer.append(source, start, len);
      return decodedBuffer.toString();
    }
    return source.substring(start, len);
  }

  private static int decodePercent(String source, int pos, int len, ByteBuffer decoderInput,
      CharBuffer decoderOutput, CharsetDecoder decoder) {
    decoderOutput.clear();
    decoderInput.clear();
    do {
      if (pos + 3 > len) {
        throw new IllegalArgumentException(
            "Unterminated escape sequence at index " + pos + " of: " + source);
      }
      decoderInput.put(decodeHexByte(source, pos + 1));
      pos += 3;
    } while (pos < len && source.charAt(pos) == '%');
    pos--;
    /** Decode using UTF-8: */
    decoderInput.flip();
    CoderResult result = decoder.decode(decoderInput, decoderOutput, true);
    try {
      if (!result.isUnderflow()) {
        result.throwException();
      }
      result = decoder.flush(decoderOutput);
      if (!result.isUnderflow()) {
        result.throwException();
      }
    } catch (CharacterCodingException ex) {
      throw Throwing.sneakyThrow(ex);
    }
    return pos;
  }

  /**
   * Helper to decode half of a hexadecimal number from a string.
   * @param c The ASCII character of the hexadecimal number to decode.
   * Must be in the range {@code [0-9a-fA-F]}.
   * @return The hexadecimal value represented in the ASCII character
   * given, or {@code -1} if the character is invalid.
   */
  public static int decodeHexNibble(final char c) {
    // Character.digit() is not used here, as it addresses a larger
    // set of characters (both ASCII and full-width latin letters).
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 0xA;
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 0xA;
    }
    return -1;
  }

  /**
   * Decode a 2-digit hex byte from within a string.
   */
  public static byte decodeHexByte(CharSequence s, int pos) {
    int hi = decodeHexNibble(s.charAt(pos));
    int lo = decodeHexNibble(s.charAt(pos + 1));
    if (hi == -1 || lo == -1) {
      throw new IllegalArgumentException(String.format(
          "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
    }
    return (byte) ((hi << 4) + lo);
  }
}
