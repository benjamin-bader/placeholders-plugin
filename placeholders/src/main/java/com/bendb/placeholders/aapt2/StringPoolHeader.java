package com.bendb.placeholders.aapt2;

import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;

class StringPoolHeader {
    public static final long FLAG_SORTED = 1 << 0;
    public static final long FLAG_UTF8 = 1 << 8;

    public ResourceChunkHeader header;
    public long stringCount; // uint32
    public long styleCount; // uint32
    public long flags; // uint32
    public long stringsStart; // uint32
    public long stylesStart; //uint32

    public StringPoolHeader(ResourceChunkHeader header, long stringCount, long styleCount, long flags, long stringsStart, long stylesStart) {
        this.header = header;
        this.stringCount = stringCount;
        this.styleCount = styleCount;
        this.flags = flags;
        this.stringsStart = stringsStart;
        this.stylesStart = stylesStart;
    }

    public static StringPoolHeader parse(Buffer buffer) {
        ResourceChunkHeader header = ResourceChunkHeader.parse(buffer);
        long stringCount      = buffer.readInt() & 0xFFFFFFFFL;
        long styleCount       = buffer.readInt() & 0xFFFFFFFFL;
        long flags            = buffer.readInt() & 0xFFFFFFFFL;
        long stringsStart     = buffer.readInt() & 0xFFFFFFFFL;
        long stylesStart      = buffer.readInt() & 0xFFFFFFFFL;

        return new StringPoolHeader(
                header,
                stringCount,
                styleCount,
                flags,
                stringsStart,
                stylesStart);
    }

    public boolean isUtf8() {
        return (this.flags & FLAG_UTF8) == FLAG_UTF8;
    }

    public void setIsUtf8(boolean isUtf8) {
        if (isUtf8) {
            this.flags |= FLAG_UTF8;
        } else {
            this.flags &= ~FLAG_UTF8;
        }
    }

    public boolean isSorted() {
        return (this.flags & FLAG_SORTED) == FLAG_SORTED;
    }

    public void setSorted(boolean sorted) {
        if (sorted) {
            this.flags |= FLAG_SORTED;
        } else {
            this.flags &= ~FLAG_SORTED;
        }
    }

    public void write(BufferedSink sink) throws IOException {
        header.write(sink);
        sink.writeIntLe((int) (stringCount  & 0xFFFFFFFFL));
        sink.writeIntLe((int) (styleCount   & 0xFFFFFFFFL));
        sink.writeIntLe((int) (flags        & 0xFFFFFFFFL));
        sink.writeIntLe((int) (stringsStart & 0xFFFFFFFFL));
        sink.writeIntLe((int) (stylesStart  & 0xFFFFFFFFL));
    }
}
