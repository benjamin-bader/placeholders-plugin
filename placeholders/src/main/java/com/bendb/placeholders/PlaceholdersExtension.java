package com.bendb.placeholders;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlaceholdersExtension {
    private Map<String, String> placeholders = new LinkedHashMap<>();

    Map<String, String> getPlaceholders() {
        return new LinkedHashMap<>(placeholders);
    }

    public void replace(Object placeholder, Object replacement) {
        placeholders.put(String.valueOf(placeholder), String.valueOf(replacement));
    }
}
