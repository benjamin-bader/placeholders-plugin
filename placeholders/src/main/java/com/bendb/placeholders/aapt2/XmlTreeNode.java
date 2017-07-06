package com.bendb.placeholders.aapt2;

public class XmlTreeNode {
    ResourceChunkHeader chunkHeader;
    long lineNumber;
    long comment; // ResStringPool_ref index; uint32_t

    XmlTreeNode(ResourceChunkHeader chunkHeader) {
        this.chunkHeader = chunkHeader;
    }
}
