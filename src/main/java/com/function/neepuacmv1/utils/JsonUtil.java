package com.function.neepuacmv1.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    private JsonUtil() {}
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(Object o) {
        try { return MAPPER.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static <T> T fromJson(String s, Class<T> clz) {
        try { return MAPPER.readValue(s, clz); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
