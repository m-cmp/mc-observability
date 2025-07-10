package com.innogrid.tabcloudit.o11ymanager.global.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class CookieJar {
    private final Map<String, String> cookies = new HashMap<>();

    public void addCookie(String name, String value) {
        cookies.put(name, value);
    }

    public String getCookiesAsString() {
        return cookies.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }
}
