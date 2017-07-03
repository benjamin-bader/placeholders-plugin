package com.bendb.placeholders.aapt2;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

/**
 * A parser that can read a {@link StringPool} binary representation.
 */
public class StringPoolParser {

    private final byte[] bytes;
    private int idx = 0;
    private boolean littleEndian = true;

    public static StringPool parse(ByteString byteString) {
        byte[] bytes = byteString.toByteArray();
        StringPoolParser parser = new StringPoolParser(bytes);
        return parser.parse();
    }

    StringPoolParser(byte[] bytes) {
        this.bytes = Preconditions.checkNotNull(bytes, "bytes");
        this.idx = 0;
    }

    StringPool parse() {
        return null;
    }

    private long parseUInt32() {
        require(4);
        throw new IllegalStateException("Not implemented");
    }

    private int parseUInt16() {
        require(2);
        throw new IllegalStateException("Not implemented");
    }

    private long parseSizeT() {
        require(4);
        throw new IllegalStateException("Not implemented");
    }

    private void require(int bytesRequired) {
        int remaining = bytes.length - idx;
        if (remaining < bytesRequired) {
            throw new IllegalStateException("Invalid StringPool; expected at least "
                    + bytesRequired + " more bytes, but only " + remaining + " remain.");
        }
    }
}
