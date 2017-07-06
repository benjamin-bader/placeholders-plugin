package com.bendb.placeholders.aapt2;

import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;

class ResourceChunkHeader {
    public int type; // uint16
    public int headerSize; // uint16
    public int size; // uint16

    public ResourceChunkHeader(int type, int headerSize, int size) {
        this.type = type;
        this.headerSize = headerSize;
        this.size = size;
    }

    public static ResourceChunkHeader parse(Buffer buffer) {
        int type = buffer.readShortLe() & 0xFFFF;
        int headerSize = buffer.readShortLe() & 0xFFFF;
        int size = buffer.readShortLe() & 0xFFFF;
        return new ResourceChunkHeader(type, headerSize, size);
    }

    public void write(BufferedSink sink) throws IOException {
        sink.writeShortLe(type & 0xFFFF);
        sink.writeShortLe(headerSize & 0xFFFF);
        sink.writeShortLe(size & 0xFFFF);
    }
}
