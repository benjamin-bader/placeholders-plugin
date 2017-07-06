package com.bendb.placeholders.aapt2;

import com.google.common.collect.Multimap;
import okio.Buffer;

import java.util.List;

/**
 * A class that can read an write the aapt2 StringPool class'
 * binary format.
 */
public class StringPool {
    // the parsed header, if non-null.
    private StringPoolHeader header;

    private List<Entry> strings;
    private List<StyleEntry> styles;
    private Multimap<String, Entry> indexedStrings;

    public static StringPool parse(byte[] bytes) {
        Buffer buffer = new Buffer();
        buffer.write(bytes);

        long sz = (buffer.readInt() & 0xFFFFFFFFL); // size_t, almost certainly 32 bits


        return null;

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
