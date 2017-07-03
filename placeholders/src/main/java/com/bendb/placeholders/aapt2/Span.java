package com.bendb.placeholders.aapt2;

public class Span {
    public final String name;
    public final long firstChar; // uint32
    public final long lastChar; // uint32

    public Span(String name, long firstChar, long lastChar) {
        this.name = name;
        this.firstChar = firstChar;
        this.lastChar = lastChar;
    }
}
