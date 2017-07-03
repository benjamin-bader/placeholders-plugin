package com.bendb.placeholders.aapt2;

import com.google.common.collect.Multimap;
import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;
import java.util.List;

/**
 * A class that can read an write the aapt2 StringPool class'
 * binary format.
 */
public class StringPool {
    // the parsed header, if non-null.
    private ResStringPoolHeader header;

    private List<Entry> strings;
    private List<StyleEntry> styles;
    private Multimap<String, Entry> indexedStrings;

    public static StringPool parse(byte[] bytes) {
        Buffer buffer = new Buffer();
        buffer.write(bytes);

        long sz = (buffer.readInt() & 0xFFFFFFFFL); // size_t, almost certainly 32 bits


        return null;

    }

    public static class ResChunkHeader {
        public int type; // uint16
        public int headerSize; // uint16
        public int size; // uint16

        public ResChunkHeader(int type, int headerSize, int size) {
            this.type = type;
            this.headerSize = headerSize;
            this.size = size;
        }

        public static ResChunkHeader parse(Buffer buffer) {
            int type = buffer.readShortLe() & 0xFFFF;
            int headerSize = buffer.readShortLe() & 0xFFFF;
            int size = buffer.readShortLe() & 0xFFFF;
            return new ResChunkHeader(type, headerSize, size);
        }

        public void write(BufferedSink sink) throws IOException {
            sink.writeShortLe(type & 0xFFFF);
            sink.writeShortLe(headerSize & 0xFFFF);
            sink.writeShortLe(size & 0xFFFF);
        }
    }

    public static class ResStringPoolHeader {
        public static final long FLAG_SORTED = 1 << 0;
        public static final long FLAG_UTF8 = 1 << 8;

        public ResChunkHeader header;
        public long stringCount; // uint32
        public long styleCount; // uint32
        public long flags; // uint32
        public long stringsStart; // uint32
        public long stylesStart; //uint32

        public ResStringPoolHeader(ResChunkHeader header, long stringCount, long styleCount, long flags, long stringsStart, long stylesStart) {
            this.header = header;
            this.stringCount = stringCount;
            this.styleCount = styleCount;
            this.flags = flags;
            this.stringsStart = stringsStart;
            this.stylesStart = stylesStart;
        }

        public static ResStringPoolHeader parse(Buffer buffer) {
            ResChunkHeader header = ResChunkHeader.parse(buffer);
            long stringCount      = buffer.readInt() & 0xFFFFFFFFL;
            long styleCount       = buffer.readInt() & 0xFFFFFFFFL;
            long flags            = buffer.readInt() & 0xFFFFFFFFL;
            long stringsStart     = buffer.readInt() & 0xFFFFFFFFL;
            long stylesStart      = buffer.readInt() & 0xFFFFFFFFL;

            return new ResStringPoolHeader(
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

    public static class Entry {
        private String value;
        private int index;
        private int ref;

        public Entry(String value, int index, int ref) {
            this.value = value;
            this.index = index;
            this.ref = ref;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public int getRef() {
            return ref;
        }
    }

    public static class StyleEntry {
        public Ref str;
        public List<Span> spans;

        private int ref;
    }

    public static class Ref {
        private Entry entry;

        public Ref(Entry entry) {
            this.entry = entry;
        }

        public Ref(Ref ref) {
            this.entry = ref.entry;
            if (this.entry != null) {
                this.entry.ref++;
            }
        }

        public String getValue() {
            return entry.getValue();
        }

        public int getIndex() {
            return entry.getIndex();
        }
    }

    public static class StyleRef {
        private StyleEntry entry;

        public StyleRef(StyleEntry entry) {
            this.entry = entry;
        }
    }
}
