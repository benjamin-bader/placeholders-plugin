package com.bendb.placeholders.aapt2;

import java.util.List;

public class StyleString {
    public final String str;
    public final List<Span> spans;

    public StyleString(String str, List<Span> spans) {
        this.str = str;
        this.spans = spans;
    }
}
