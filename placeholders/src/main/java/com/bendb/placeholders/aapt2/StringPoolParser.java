package com.bendb.placeholders.aapt2;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;

/**
 * A parser that can read a {@link StringPool} binary representation.
 */
public class StringPoolParser {

    private static final int ENCODED_BYTE_LEN_MASK = 1 << 7;
    private static final int ENCODED_SHORT_LEN_MASK = 1 << 15;
    private static final int ENCODED_MAX_LEN = ENCODED_BYTE_LEN_MASK - 1;

    private final byte[] bytes;
    private int idx = 0;
    private boolean littleEndian = true;

    public static StringPool parse(ByteString byteString) {
        byte[] bytes = byteString.toByteArray();
        StringPoolParser parser = new StringPoolParser(bytes);
        return parser.parse();
    }

    StringPoolParser(byte[] bytes) {
        this.bytes = bytes;// Preconditions.checkNotNull(bytes, "bytes");
        this.idx = 0;
    }

    StringPool parse() {
        StringPoolHeader header = parsePoolHeader();

        for (long i = 0; i < header.stringCount; ++i) {
            String entry;
            if (header.isUtf8()) {
                int numChars = readEncodedLengthForChar8();
                int numEncodedBytes = readEncodedLengthForChar8();
                entry = new String(bytes, idx, numEncodedBytes, StandardCharsets.UTF_8);
                if (entry.length() != numChars) {
                    throw new IllegalStateException("Malformed entry; expected " + numChars + " chars, read " + entry.length());
                }
            } else {
                int numChars = readEncodedLengthForChar16();
                int numBytes = numChars * 2;
                entry = new String(bytes, idx, numBytes, StandardCharsets.UTF_16);
                if (entry.length() != numChars) {
                    throw new IllegalStateException("Malformed entry; expected " + numChars + " chars, read " + entry.length());
                }
            }

            System.out.println("" + i + ": " + entry);
        }

        return null;
    }

    private StringPoolHeader parsePoolHeader() {
        ResourceChunkHeader chunkHeader = parseChunkHeader();
        long stringCount  = parseUInt32();
        long styleCount   = parseUInt32();
        long flags        = parseUInt32();
        long stringsStart = parseUInt32();
        long stylesStart  = parseUInt32();
        return new StringPoolHeader(chunkHeader, stringCount, styleCount, flags, stringsStart, stylesStart);
    }

    private ResourceChunkHeader parseChunkHeader() {
        return new ResourceChunkHeader(parseUInt16(), parseUInt16(), parseUInt16());
    }

    private long parseUInt32() {
        require(4);
        long uint32
                = ((bytes[idx++] & 0xFF) << 24)
                | ((bytes[idx++] & 0xFF) << 16)
                | ((bytes[idx++] & 0xFF) << 8)
                |  (bytes[idx++] & 0xFF);
        if (littleEndian) {
            uint32
                    = ((uint32 & 0xFF000000) >>> 24)
                    | ((uint32 & 0x00FF0000) >>> 8)
                    | ((uint32 & 0x0000FF00) << 8)
                    | ((uint32 & 0x000000FF) << 24);
        }
        return uint32;
    }

    private int parseUInt16() {
        require(2);
        int uint16
                = ((bytes[idx++] & 0xFF) << 8)
                |  (bytes[idx++] & 0xFF);
        if (littleEndian) {
            uint16
                    = ((uint16 & 0xFF00) >>> 8)
                    | ((uint16 & 0x00FF) << 8);
        }
        return uint16;
    }

    private long parseSizeT() {
        return parseUInt32();
    }

    /**
     * Advances the index to the next 4-byte-aligned boundary
     */
    private void align4() {
        int remainder = idx % 4;
        if (remainder != 0) {
            int adjust = 4 - remainder;
            idx = Math.min(bytes.length, idx + adjust);
        }
    }

    private int readEncodedLengthForChar8() {
        int len = 0;
        while (idx < bytes.length) {
            int b = bytes[idx++] & 0xFF;
            len <<= 8;
            len += (b & ~ENCODED_BYTE_LEN_MASK);
            if ((b & ENCODED_BYTE_LEN_MASK) == ENCODED_BYTE_LEN_MASK) {
                return len;
            }
        }
        throw new IllegalStateException("Unexpected end of input reading encoded length");
    }

    private int readEncodedLengthForChar16() {
        int len = 0;
        while (idx + 1 < bytes.length) {
            int c = ((bytes[idx++] & 0xFF) << 8) | (bytes[idx++] & 0xFF);
            len <<= 16;
            len += (c & ~ENCODED_SHORT_LEN_MASK);
            if ((c & ENCODED_SHORT_LEN_MASK) == ENCODED_SHORT_LEN_MASK) {
                return len;
            }
        }
        throw new IllegalStateException("Unexpected end of input reading encoded length");
    }

    private static int swapIntEndianness(int x) {
        return ((x & 0xFF000000) >>> 24)
             | ((x & 0x00FF0000) >>> 8)
             | ((x & 0x0000FF00) << 8)
             | ((x & 0x000000FF) << 24);
    }

    private static long swapLongEndianness(long l) {
        throw new AssertionError("not implemented");
    }

    private void require(int bytesRequired) {
        int remaining = bytes.length - idx;
        if (remaining < bytesRequired) {
            throw new IllegalStateException("Invalid StringPool; expected at least "
                    + bytesRequired + " more bytes, but only " + remaining + " remain.");
        }
    }
}
